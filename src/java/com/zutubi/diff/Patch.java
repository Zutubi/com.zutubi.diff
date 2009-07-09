package com.zutubi.diff;

import java.io.File;

/**
 * Represents a patch to a single file.  A larger patch file may contain
 * several patches.
 *
 * @see PatchFile
 */
public interface Patch
{
    /**
     * @return the path of the old file that was changed
     */
    String getOldFile();

    /**
     * @return the path of the new file that was created
     */
    String getNewFile();

    /**
     * @return the type of patch: what sort of operation it applies
     */
    PatchType getType();

    /**
     * Apply this patch to the given file.  The changes represented by this
     * patch are applied in place to the file.  The patch must apply cleanly,
     * e.g. for a unified patch all common lines, including context, must be
     * found exactly where specified by the patch (no fuzzy patching is done).
     *
     * @param file the file to patch
     * @throws PatchApplyException if the patch does not apply cleanly (likely
     *         because the file differs from the old file in the diff) or an
     *         I/O error occurs
     */
    void apply(File file) throws PatchApplyException;
}
