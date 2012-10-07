package com.zutubi.diff.git;

import com.zutubi.diff.PatchParseException;

import java.util.List;

/**
 * A git patch for a binary file -- using a git-specific format.  Currently
 * these patches are only parsed for their header information -- the data is
 * not read nor can the patch be applied.
 */
public class GitBinaryPatch extends GitPatch
{
    /**
     * Creates a new git binary patch.
     *
     * @param lineNumber   line number where this patch was found in a larger
     *                     patch file
     * @param headerLine   first line of the patch (diff --git ...)
     * @param extendedInfo extended header lines found between the header line
     *                     and the unified diff (i.e. before the GIT binary
     *                     diff line).
     * @throws PatchParseException if there is an error interpreting the header
     *                             lines
     */
    public GitBinaryPatch(int lineNumber, String headerLine, List<String> extendedInfo) throws PatchParseException
    {
        super(lineNumber, headerLine, extendedInfo);
    }
}
