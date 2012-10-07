package com.zutubi.diff;

/**
 * Exception raised on error applying a patch.
 *
 * @see PatchFile
 */
public class PatchApplyException extends DiffException
{
    public PatchApplyException()
    {
    }

    public PatchApplyException(String message)
    {
        super(message);
    }

    public PatchApplyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PatchApplyException(Throwable cause)
    {
        super(cause);
    }
}