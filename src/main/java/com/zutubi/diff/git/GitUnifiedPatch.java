package com.zutubi.diff.git;

import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.unified.UnifiedHunk;
import com.zutubi.diff.unified.UnifiedPatch;

import java.util.List;

/**
 * A git patch for a text file, which is essentially a unified diff plus some
 * extended info.
 */
public class GitUnifiedPatch extends GitPatch
{
    private UnifiedPatch unified;

    /**
     * Creates a new git unified patch, wrapping an underlying standard unified
     * patch.
     *
     * @param lineNumber   line number where this patch was found in a larger
     *                     patch file
     * @param headerLine   first line of the patch (diff --git ...)
     * @param extendedInfo extended header lines found between the header line
     *                     and the unified diff (i.e. before the
     *                      --- &lt;old file&gt; line).
     * @param unified      the underlying unified patch
     * @throws PatchParseException if there is an error interpreting the header
     *                             lines
     */
    public GitUnifiedPatch(int lineNumber, String headerLine, List<String> extendedInfo, UnifiedPatch unified) throws PatchParseException
    {
        super(lineNumber, headerLine, extendedInfo);
        this.unified = unified;
    }

    /**
     * @see com.zutubi.diff.unified.UnifiedPatch#getHunks()
     */
    public List<UnifiedHunk> getHunks()
    {
        return unified.getHunks();
    }

    /**
     * @see com.zutubi.diff.unified.UnifiedPatch#isNewlineTerminated()
     */
    public boolean isNewlineTerminated()
    {
        return unified.isNewlineTerminated();
    }
}
