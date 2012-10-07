package com.zutubi.diff.git;

import com.zutubi.diff.Patch;
import com.zutubi.diff.PatchApplyException;
import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.PatchType;
import com.zutubi.diff.util.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Base for git patches, which holds and can interpret the extended information
 * in the git header.
 */
public abstract class GitPatch implements Patch
{
    private static final String INFO_DELETED = "deleted file";
    private static final String INFO_NEW = "new file";
    private static final String INFO_COPY_FROM = "copy from";
    private static final String INFO_COPY_TO = "copy to";
    private static final String INFO_RENAME_FROM = "rename from";
    private static final String INFO_RENAME_TO = "rename to";

    private int lineNumber;
    private String headerLine;
    private List<String> extendedInfo;
    private String oldFile;
    private String newFile;
    private PatchType type;

    /**
     * Create a new git patch found at the given line number, with the given
     * header line and extended info.  The header line and extended info are
     * processed to determine the patch type, old file and new file.
     *
     * @param lineNumber   line number in a larger patch file where this patch
     *                     was found
     * @param headerLine   the first line of the patch (starts with diff --git)
     * @param extendedInfo all lines after the header, but before the content
     *                     of the patch (what the git diff man page calls
     *                     "extended header lines").
     * @throws PatchParseException if there is an error processing the headers,
     *                             including the inability to extract the old
     *                             or new file name
     */
    protected GitPatch(int lineNumber, String headerLine, List<String> extendedInfo) throws PatchParseException
    {
        this(lineNumber, headerLine, extendedInfo, PatchType.EDIT);
    }

    /**
     * Create a new git patch found at the given line number, with the given
     * header line, extended info and default type.  The header line and
     * extended info are processed to determine the patch type, old file and
     * new file.
     *
     * @param lineNumber   line number in a larger patch file where this patch
     *                     was found
     * @param headerLine   the first line of the patch (starts with diff --git)
     * @param extendedInfo all lines after the header, but before the content
     *                     of the patch (what the git diff man page calls
     *                     "extended header lines").
     * @param defaultType  the default patch type, used if the extended headers
     *                     don't imply another type
     * @throws PatchParseException if there is an error processing the headers,
     *                             including the inability to extract the old
     *                             or new file name
     */
    protected GitPatch(int lineNumber, String headerLine, List<String> extendedInfo, PatchType defaultType) throws PatchParseException
    {
        this.lineNumber = lineNumber;
        this.headerLine = headerLine;
        this.extendedInfo = new LinkedList<String>(extendedInfo);
        this.type = defaultType;

        String filename = readFilename();
        if (filename != null)
        {
            oldFile = newFile = filename;
        }

        int line = lineNumber;
        for (String info: extendedInfo)
        {
            line++;
            processInfo(line, info);
        }

        if (oldFile == null)
        {
            throw new PatchParseException(lineNumber, "Unable to determine old file name for git patch");
        }

        if (newFile == null)
        {
            throw new PatchParseException(lineNumber, "Unable to determine new file name for git patch");
        }
    }

    private String readFilename() throws PatchParseException
    {
        String files = StringUtils.stripPrefix(headerLine, GitPatchParser.HEADER_PREFIX).trim();

        // We can only reliably parse this if the a/ and b/ names are the same,
        // which they are except in the case where a rename or copy has
        // occurred.  Luckily, in these cases, we can extract the names from
        // the extended info.
        int length = files.length();
        if (length > 6 && (length % 2) == 1)
        {
            int middleSpaceIndex = length / 2;
            String first = files.substring(0, middleSpaceIndex);
            String second = files.substring(middleSpaceIndex + 1);

            // Check the only difference is the initial a is a b.
            int aIndex = first.indexOf('a');
            if (aIndex >= 0 && second.charAt(aIndex) == 'b' && first.substring(aIndex + 1).equals(second.substring(aIndex + 1)))
            {
                return StringUtils.stripPrefix(unquoteIfNecessary(lineNumber, first), "a/");
            }
        }

        return null;
    }

    private String unquoteIfNecessary(int line, String file) throws PatchParseException
    {
        if (file.startsWith("\""))
        {
            return unquote(line, file);
        }
        else
        {
            return file;
        }
    }

    private String unquote(int line, String file) throws PatchParseException
    {
        StringBuilder result = new StringBuilder(file.length());
        boolean escaped = false;

        for (int i = 1; i < file.length(); i++)
        {
            char c = file.charAt(i);

            if (escaped)
            {
                escaped = false;
                switch (c)
                {
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case '"':
                        result.append('"');
                        break;
                    default:
                        result.append(c);
                }
            }
            else
            {
                switch (c)
                {
                    case '\\':
                        escaped = true;
                        break;
                    case '"':
                        return result.toString();
                    default:
                        result.append(c);
                }
            }
        }

        throw new PatchParseException(line, "Unmatched quotes in git diff header");
    }

    private void processInfo(int line, String info) throws PatchParseException
    {
        if (info.startsWith(INFO_DELETED))
        {
            type = PatchType.DELETE;
        }
        else if (info.startsWith(INFO_NEW))
        {
            type = PatchType.ADD;
        }
        else if (info.startsWith(INFO_COPY_FROM))
        {
            type = PatchType.COPY;
            oldFile = unquoteIfNecessary(line, StringUtils.stripPrefix(info, INFO_COPY_FROM).trim());
        }
        else if (info.startsWith(INFO_COPY_TO))
        {
            type = PatchType.COPY;
            newFile = unquoteIfNecessary(line, StringUtils.stripPrefix(info, INFO_COPY_TO).trim());
        }
        else if (info.startsWith(INFO_RENAME_FROM))
        {
            type = PatchType.RENAME;
            oldFile = unquoteIfNecessary(line, StringUtils.stripPrefix(info, INFO_RENAME_FROM).trim());
        }
        else if (info.startsWith(INFO_RENAME_TO))
        {
            type = PatchType.RENAME;
            newFile = unquoteIfNecessary(line, StringUtils.stripPrefix(info, INFO_RENAME_TO).trim());
        }
    }

    /**
     * Returns the extended header lines in this patch (between the diff --git
     * line and the patch content).
     *
     * @return extended info lines (unmodifiable)
     */
    public List<String> getExtendedInfo()
    {
        return Collections.unmodifiableList(extendedInfo);
    }

    public PatchType getType()
    {
        return type;
    }

    public String getOldFile()
    {
        return oldFile;
    }

    public String getNewFile()
    {
        return newFile;
    }

    public void apply(File file) throws PatchApplyException
    {
        throw new PatchApplyException("Application of git patches is not yet supported (apply them using GitClient)");
    }
}
