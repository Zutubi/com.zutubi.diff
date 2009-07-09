package com.zutubi.diff;

/**
 * Types of patches.  Indicates what kind of operation the patch will apply to
 * its path: e.g. create a new file ({@link #ADD}), update an existing file
 * ({@link #EDIT}) etc.
 */
public enum PatchType
{
    ADD(true, false),
    COPY(true, false),
    EDIT(false, false),
    DELETE(false, true),
    METADATA(false, false),
    RENAME(true, false);

    private boolean fileCreated;
    private boolean fileRemoved;

    PatchType(boolean fileCreated, boolean fileRemoved)
    {
        this.fileCreated = fileCreated;
        this.fileRemoved = fileRemoved;
    }

    /**
     * Indicates if this patch type implies creation of a new file.
     *
     * @return true if patches of this type create a new file
     */
    public boolean isFileCreated()
    {
        return fileCreated;
    }

    /**
     * Indicates if this patch type implies removal of an existing file.
     *
     * @return true if patches of this type remove an existing file
     */
    public boolean isFileRemoved()
    {
        return fileRemoved;
    }
}
