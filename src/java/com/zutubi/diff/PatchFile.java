package com.zutubi.diff;

import com.zutubi.util.Pair;
import com.zutubi.util.TextUtils;
import com.zutubi.util.io.IOUtils;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Represents a patch file, a collection of unified diffs in one file in the
 * format expected by the patch utility.
 * </p>
 * <p>
 * A patch file is made up a a sequence of file patches, each of which can be
 * identified by a header:
 *
 * <pre>
 * '--- ' old-filename '\t' date ' ' time ' ' timezone
 * '+++ ' new-filename '\t' date ' ' time ' ' timezone
 * </pre>
 *
 * Where:
 *
 * <ul>
 *     <li>dates are formatted YYYY-MM-DD</li>
 *     <li>times are 24-hour, and formatted HH:MM:SS.FFFFFFFFF (fractional seconds)</li>
 *     <li>timezones are relative to GMT, formatted ('+' | '-') HHMM (e.g. -0800)</li>
 * </ul>

 * The header may be preceded by extra lines that are tool-specific, and should
 * be ignored if not recognised.  Following the header comes a sequence of
 * change hunks to apply to the file.  Each hunk  has the form:
 *
 * <pre>
 * '@@ -' old-range ' +' new-range ' @@'
 * </pre>
 *
 * where ranges take one of two forms:
 *
 * <pre>
 * uint | uint ',' uint
 * </pre>

 * In the first form the second unsigned int is implicitly take to be 1.  The
 * ranges indicate the location and size of the hunk in each file.  The first
 * number is the one-based line offset of the start of the hunk, and the second
 * number is the number of lines in the file covered by that hunk.
 * </p>
 * <p>
 * Hunk headers are followed by the lines of the hunk.  Each line is preceded
 * by a ' ', '+' or '-' which mean that the line is common, only in the new
 * file or only in the old file respectively.  Common lines occur both as
 * context at the start and end of the hunk (the number of lines being
 * configurable when creating the diff), and possibly within the diff (as
 * overlapping context will cause two hunks to be combined into one).
 * </p>
 * <p>
 * There are lots of other variations/rules to be aware of:
 *
 * <ul>
 *     <li>If there is no newline at the end of the file, the hunk line will
 *     still end with a newline, but the immediately following line will be
 *     '\ No newline at end of file'</li>
 *     <li>Several tools use their own format for everything after the filename
 *     in the header, e.g. Subversion has '(' revision ')' instead of the
 *     timestamps.  Thus the timestamps cannot be relied upon.</li>
 *     <li>Added files can be identified by an old-range of 0,0, i.e. a hunk
 *     header something like @@ -0,0 +1,12 @@</li>
 *     <li>Deleted files can almost be identified by a new-range of 0,0 -- but
 *     this does not differentiate them from a file which is made empty.  To
 *     make this differentiation one can look at the new timestamp (if
 *     provided), as it will be set to the epoch for a deleted file but not an
 *     emptied one.  If using Subversion, the revision is given as 0 for a
 *     deleted file, but not an emptied one.</li>
 * </ul>
 */
public class PatchFile
{
    private static final Pattern RE_EPOCH = Pattern.compile(".*(1970-01-01|\\(revision 0\\)).*");
    private static final Pattern RE_HUNK = Pattern.compile("\\s*@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@\\s*");

    private List<Patch> patches = new LinkedList<Patch>();

    /**
     * Returns a list of patches, one for each changed file in this patch file.
     *
     * @return the patches in this patch file (unmodifiable)
     */
    public List<Patch> getPatches()
    {
        return Collections.unmodifiableList(patches);
    }

    /**
     * Apply this patch to files within the given directory.  The equivalent of
     * the Unix patch command.  Only supports clean patches - no "fuzz" is
     * allowed.
     * 
     * @param baseDir          base directory used to locate files to patch
     *                         (equivalent to the working directory or -d flag
     *                         to patch)
     * @param prefixStripCount number of path elements to strip from the paths
     *                         of patches when locating files to patch
     *                         (equivalent to the -p option to patch)
     * @throws PatchApplyException if the patch does not apply cleanly or an
     *         I/O error occurs
     */
    public void apply(File baseDir, int prefixStripCount) throws PatchApplyException
    {
        for (Patch patch: patches)
        {
            File destinationFile = resolveDestination(baseDir, patch.getNewFile(), prefixStripCount);
            if (!patch.isAdded() && !destinationFile.exists())
            {
                throw new PatchApplyException("Expected destination file '" + destinationFile.getAbsolutePath() + "' does not exist");
            }

            patch.apply(destinationFile);
        }
    }

    private File resolveDestination(File baseDir, String newFile, int prefixStripCount)
    {
        newFile = newFile.replace('\\', '/');
        int offset = 0;
        int i = 0;
        while (prefixStripCount-- > 0)
        {
            i = newFile.indexOf('/', offset);
            if (i < 0 || i == newFile.length())
            {
                break;
            }

            offset = i + 1;
        }

        return new File(baseDir, newFile.substring(i));
    }

    /**
     * Parses a patch file from the given input reader.  The input patch file
     * should be in unified diff format (see class comment).  The parsing is
     * permissive - ignoring unexpected lines where possible, and supporting
     * known variations to the diff format.
     *
     * @param input input to read the patch content from - ownership is take
     *              and this method will close the reader
     * @return the parsed patch file
     * @throws PatchParseException on an unrecoverable parse or I/O error
     */
    public static PatchFile read(Reader input) throws PatchParseException
    {
        PatchReader patchReader = null;
        try
        {
            patchReader = new PatchReader(input);
            return read(patchReader);
        }
        catch (IOException e)
        {
            throw new PatchParseException(-1, "I/O error reading patch: " + e.getMessage(), e);
        }
        finally
        {
            IOUtils.close(patchReader);
        }
    }

    private static PatchFile read(PatchReader reader) throws IOException, PatchParseException
    {
        PatchFile patchFile = new PatchFile();
        while (!reader.spent())
        {
            // Look for the patch header ---, skipping anything extra that may
            // be in the way.
            String line = reader.peek();
            if (line.startsWith(Patch.HEADER_OLD_FILE))
            {
                patchFile.patches.add(readPatch(reader));
            }
            else
            {
                reader.next();
            }
        }

        return patchFile;
    }

    private static Patch readPatch(PatchReader reader) throws IOException, PatchParseException
    {
        // Headers are:
        //
        //   '--- ' old-filename '\t' date ' ' time ' ' timezone
        //   '+++ ' new-filename '\t' date ' ' time ' ' timezone
        //
        // The portion after the filenames is optional - and may have a
        // completely different format.
        Pair<String, Boolean> oldHead = parsePatchHeader(reader, Patch.HEADER_OLD_FILE);
        Pair<String, Boolean> newHead = parsePatchHeader(reader, Patch.HEADER_NEW_FILE);

        Patch patch = new Patch(oldHead.first, oldHead.second, newHead.first, newHead.second);
        while (!reader.spent())
        {
            String line = reader.peek();
            if (line.startsWith(Patch.HEADER_OLD_FILE))
            {
                // Hit the next patch.
                break;
            }
            else
            {
                reader.next();

                Matcher matcher = RE_HUNK.matcher(line);
                if (matcher.matches())
                {
                    int hunkLineNumber = reader.getLineNumber();
                    long oldOffset = Long.parseLong(matcher.group(1));
                    long oldLength = parseOptionalLong(matcher.group(2));
                    long newOffset = Long.parseLong(matcher.group(3));
                    long newLength = parseOptionalLong(matcher.group(4));

                    Hunk hunk = new Hunk(oldOffset, oldLength, newOffset, newLength);
                    if (readHunkLines(reader, hunk))
                    {
                        patch.setNewlineTerminated(false);
                    }

                    if (checkConsistency(patch, hunk, hunkLineNumber))
                    {
                        patch.addHunk(hunk);
                    }
                }
            }
        }

        return patch;
    }

    private static Pair<String, Boolean> parsePatchHeader(PatchReader reader, String indicator) throws IOException, PatchParseException
    {
        String line = reader.next();
        if (!line.startsWith(indicator))
        {
            throw new PatchParseException(reader.getLineNumber(), "Expecting a line starting with '" + indicator + "'; got '" + line + "'");
        }

        String trim = line.substring(indicator.length()).trim();
        String[] parts = trim.split("\\t", 2);
        String filename = parts[0];
        if (parts[0].length() == 0)
        {
            throw new PatchParseException(reader.getLineNumber(), "Patch header line '" + line + "' is missing filename");
        }

        boolean isEpoch = parts.length == 2 && RE_EPOCH.matcher(parts[1]).matches();
        return new Pair<String, Boolean>(filename, isEpoch);
    }

    private static long parseOptionalLong(String s)
    {
        if (TextUtils.stringSet(s))
        {
            return Long.parseLong(s);
        }
        else
        {
            return 1;
        }
    }

    private static boolean readHunkLines(PatchReader reader, Hunk hunk) throws IOException, PatchParseException
    {
        boolean previousWasNoNewline = false;
        while (!reader.spent())
        {
            String line = reader.peek();
            if (line.length() > 0)
            {
                char first = line.charAt(0);
                if (first == '\\' && line.equalsIgnoreCase(Patch.NO_NEWLINE))
                {
                    previousWasNoNewline = true;
                }
                else
                {
                    Hunk.LineType lineType = Hunk.LineType.valueOf(first);
                    if (lineType == null)
                    {
                        break;
                    }

                    previousWasNoNewline = false;
                    hunk.addLine(lineType, line.substring(1));
                }
            }

            reader.next();
        }

        return previousWasNoNewline;
    }

    private static boolean checkConsistency(Patch patch, Hunk hunk, int lineNumber) throws PatchParseException
    {
        int oldCount = 0;
        int newCount = 0;

        // Check this hunk comes after the previous one.
        List<Hunk> previousHunks = patch.getHunks();
        if (!previousHunks.isEmpty())
        {
            Hunk previousHunk = previousHunks.get(previousHunks.size() - 1);
            if (hunk.getOldOffset() < previousHunk.getOldOffset() + previousHunk.getOldLength())
            {
                throw new PatchParseException(lineNumber, "Hunk old offset " + hunk.getOldOffset() + " comes before the end of the previous hunk in the old file");
            }

            if (hunk.getNewOffset() < previousHunk.getNewOffset() + previousHunk.getNewLength())
            {
                throw new PatchParseException(lineNumber, "Hunk new offset " + hunk.getOldOffset() + " comes before the end of the previous hunk in the new file");
            }
        }

        // Check this hunk is internally consistent.
        for (Hunk.Line line: hunk.getLines())
        {
            Hunk.LineType type = line.getType();
            if (type.inOriginal())
            {
                oldCount++;
            }

            if (type.inNew())
            {
                newCount++;
            }
        }

        if (oldCount != hunk.getOldLength())
        {
            throw new PatchParseException(lineNumber, "Hunk old length " + hunk.getOldLength() + " is not consistent with hunk lines");
        }

        if (newCount != hunk.getNewLength())
        {
            throw new PatchParseException(lineNumber, "Hunk new length " + hunk.getOldLength() + " is not consistent with hunk lines");
        }

        // Return true if this hunk is empty and can be ignored.
        return oldCount + newCount > 0;
    }

    private static class PatchReader implements Closeable
    {
        private BufferedReader delegate;
        private String nextLine;
        private int lineNumber;

        private PatchReader(Reader reader) throws IOException
        {
            delegate = new BufferedReader(reader);
            nextLine = delegate.readLine();
            lineNumber = 1;
        }

        public String peek()
        {
            return nextLine;
        }

        public String next() throws IOException, PatchParseException
        {
            if (spent())
            {
                throw new PatchParseException(lineNumber, "Unexpected end of input");
            }

            String result = nextLine;
            nextLine = delegate.readLine();
            lineNumber++;
            return result;
        }

        public boolean spent()
        {
            return nextLine == null;
        }

        public int getLineNumber()
        {
            return lineNumber;
        }

        public void close() throws IOException
        {
            delegate.close();
        }
    }
}
