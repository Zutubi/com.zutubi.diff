package com.zutubi.diff.git;

import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.PatchParser;
import com.zutubi.diff.PeekReader;
import com.zutubi.diff.unified.UnifiedPatch;
import com.zutubi.diff.unified.UnifiedPatchParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A patch parser that can process diffs from "git diff --binary".  These are
 * similar to unified diffs, but add extended headers and binary patchs.
 * <p/>
 * Here is a relevant exceprt from the git diff man page:
 * <pre>
 * --------------------------------------------------------------------------
 *  What the -p option produces is slightly different from the traditional
 *  diff format.
 *
 *   1.  It is preceded with a "git diff" header, that looks like this:
 *
 *          diff --git a/file1 b/file2
 *      The a/ and b/ filenames are the same unless rename/copy is
 *      involved. Especially, even for a creation or a deletion, /dev/null
 *      is _not_ used in place of a/ or b/ filenames.
 *
 *      When rename/copy is involved, file1 and file2 show the name of the
 *      source file of the rename/copy and the name of the file that
 *      rename/copy produces, respectively.
 *
 *   2.  It is followed by one or more extended header lines:
 *
 *          old mode &lt;mode&gt;
 *          new mode &lt;mode&gt;
 *          deleted file mode &lt;mode&gt;
 *          new file mode &lt;mode&gt;
 *          copy from &lt;path&gt;
 *          copy to &lt;path&gt;
 *          rename from &lt;path&gt;
 *          rename to &lt;path&gt;
 *          similarity index &lt;number&gt;
 *          dissimilarity index &lt;number&gt;
 *          index &lt;hash&gt;..&lt;hash&gt; &lt;mode&gt;
 *
 *   3.  TAB, LF, double quote and backslash characters in pathnames are
 *      represented as \t, \n, \" and \\, respectively. If there is need
 *      for such substitution then the whole pathname is put in double
 *      quotes.
 *  The similarity index is the percentage of unchanged lines, and the
 *  dissimilarity index is the percentage of changed lines. It is a rounded
 *  down integer, followed by a percent sign. The similarity index value of
 *  100% is thus reserved for two equal files, while 100% dissimilarity
 *  means that no line from the old file made it into the new one.
 * --------------------------------------------------------------------------
 *  </pre>
 */
public class GitPatchParser implements PatchParser
{
    static String HEADER_PREFIX = "diff --git";

    private static final String BINARY_HEADER = "GIT binary patch";
    private static final Pattern PATTERN_BINARY_HUNK_HEADER = Pattern.compile("(literal|delta).*[0-9]+");

    private UnifiedPatchParser unifiedPatchParser = new UnifiedPatchParser()
    {
        @Override
        public boolean isPatchHeader(String line)
        {
            return GitPatchParser.this.isPatchHeader(line);
        }
    };

    public boolean isPatchHeader(String line)
    {
        return line.startsWith(HEADER_PREFIX);
    }

    public boolean isPatchFile(File patchFile)
    {
        try
        {
            // Check the first hundred lines for anything that looks like a git
            // diff header.
            BufferedReader reader = new BufferedReader(new FileReader(patchFile));
            for (int i = 0; i < 100; i++)
            {
                String line = reader.readLine();
                if (line == null)
                {
                    break;
                }

                if (line.startsWith("diff --git"))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            // Fall through.
        }

        return false;
    }
    
    public GitPatch parse(PeekReader reader) throws IOException, PatchParseException
    {
        String headerLine = reader.next();
        int lineNumber = reader.getLineNumber();

        // Skip all lines until we see the start of a unified or binary diff.
        List<String> extendedInfo = new LinkedList<String>();
        while (true)
        {
            String line = reader.peek();
            if (reader.spent() || isPatchHeader(line))
            {
                // No content: could be a copy, rename or pure mode change.
                return new GitTrivialPatch(lineNumber, headerLine, extendedInfo);
            }
            else if (line.startsWith(UnifiedPatch.HEADER_OLD_FILE))
            {
                return new GitUnifiedPatch(lineNumber, headerLine, extendedInfo, unifiedPatchParser.parse(reader));
            }
            else if (line.startsWith(BINARY_HEADER))
            {
                return parseBinary(reader, lineNumber, headerLine, extendedInfo);
            }
            else
            {
                extendedInfo.add(line);
                reader.next();
            }
        }
    }

    private GitBinaryPatch parseBinary(PeekReader reader, int lineNumber, String headerLine, List<String> extendedInfo) throws IOException, PatchParseException
    {
        // Binary patches have the form:

        // GIT binary patch
        // (delta|literal) <original length>
        // <base 85 encoded data>
        //
        // (delta|literal) <original length>
        // <base 85 encoded data>

        // Note that there are actually two "diffs", one for file1 vs file2 and
        // the vice-versa (file2 vs file1).  They may be delta (only a change)
        // or literal (the entire file content), depending on which is shorter.
        // Since we don't use the data at the moment we just throw it away.
        reader.next();
        processBinaryHunk(reader);
        processBinaryHunk(reader);
        return new GitBinaryPatch(lineNumber, headerLine, extendedInfo);
    }

    private void processBinaryHunk(PeekReader reader) throws IOException, PatchParseException
    {
        String line = reader.next();
        if (!PATTERN_BINARY_HUNK_HEADER.matcher(line).matches())
        {
            throw new PatchParseException(reader.getLineNumber(), "Expecting binary hunk header, got '" + line + "'");
        }

        line = reader.next();
        while (!reader.spent() && line.trim().length() > 0)
        {
            line = reader.next();
        }
    }
}
