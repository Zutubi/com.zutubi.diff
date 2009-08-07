package com.zutubi.diff.unified;

import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.PatchParser;
import com.zutubi.diff.PeekReader;
import com.zutubi.util.Pair;
import com.zutubi.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A parser that can read standard unified diffs from one file  -- in the
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
 *
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
 *
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
public class UnifiedPatchParser implements PatchParser
{
    private static final Pattern RE_EPOCH = Pattern.compile(".*(1970-01-01|\\(revision 0\\)).*");
    private static final Pattern RE_HUNK = Pattern.compile("\\s*@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@\\s*");

    public boolean isPatchHeader(String line)
    {
        return line.startsWith(UnifiedPatch.HEADER_OLD_FILE);
    }

    public UnifiedPatch parse(PeekReader reader) throws IOException, PatchParseException
    {
        // Headers are:
        //
        //   '--- ' old-filename '\t' date ' ' time ' ' timezone
        //   '+++ ' new-filename '\t' date ' ' time ' ' timezone
        //
        // The portion after the filenames is optional - and may have a
        // completely different format.
        Pair<String, Boolean> oldHead = parsePatchHeader(reader, UnifiedPatch.HEADER_OLD_FILE);
        Pair<String, Boolean> newHead = parsePatchHeader(reader, UnifiedPatch.HEADER_NEW_FILE);

        UnifiedPatch patch = new UnifiedPatch(oldHead.first, oldHead.second, newHead.first, newHead.second);
        while (!reader.spent())
        {
            String line = reader.peek();
            if (isPatchHeader(line))
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

                    UnifiedHunk hunk = new UnifiedHunk(oldOffset, oldLength, newOffset, newLength);
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

    private Pair<String, Boolean> parsePatchHeader(PeekReader reader, String indicator) throws IOException, PatchParseException
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

    private long parseOptionalLong(String s)
    {
        if (StringUtils.stringSet(s))
        {
            return Long.parseLong(s);
        }
        else
        {
            return 1;
        }
    }

    private boolean readHunkLines(PeekReader reader, UnifiedHunk hunk) throws IOException, PatchParseException
    {
        boolean previousWasNoNewline = false;
        while (!reader.spent())
        {
            String line = reader.peek();
            if (line.length() > 0)
            {
                char first = line.charAt(0);
                if (first == '\\' && line.equalsIgnoreCase(UnifiedPatch.NO_NEWLINE))
                {
                    previousWasNoNewline = true;
                }
                else
                {
                    UnifiedHunk.LineType lineType = UnifiedHunk.LineType.valueOf(first);
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

    private boolean checkConsistency(UnifiedPatch patch, UnifiedHunk hunk, int lineNumber) throws PatchParseException
    {
        int oldCount = 0;
        int newCount = 0;

        // Check this hunk comes after the previous one.
        List<UnifiedHunk> previousHunks = patch.getHunks();
        if (!previousHunks.isEmpty())
        {
            UnifiedHunk previousHunk = previousHunks.get(previousHunks.size() - 1);
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
        for (UnifiedHunk.Line line: hunk.getLines())
        {
            UnifiedHunk.LineType type = line.getType();
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
}
