package com.zutubi.diff.git;

import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.PatchType;

import java.util.List;

/**
 * A git patch that has no content, just a header or extended headers.  These
 * patches include renames, copies and pure mode changes.
 */
public class GitTrivialPatch extends GitPatch
{
    /**
     * Create a new git trivial found at the given line number, with the given
     * header line and extended info.  The header line and extended info are
     * processed to determine the patch type, old file and new file.
     *
     * @param lineNumber   line number in a larger patch file where this patch
     *                     was found
     * @param headerLine   the first line of the patch (starts with diff --git)
     * @param extendedInfo all lines after the header, but before the content
     *                     of the patch (what the git diff man page calls
     *                     "extended header lines").
     * @throws com.zutubi.diff.PatchParseException if there is an error
     *         processing the headers, including the inability to extract the
     *         old or new file name
     */
    public GitTrivialPatch(int lineNumber, String headerLine, List<String> extendedInfo) throws PatchParseException
    {
        super(lineNumber, headerLine, extendedInfo, PatchType.METADATA);
    }
}
