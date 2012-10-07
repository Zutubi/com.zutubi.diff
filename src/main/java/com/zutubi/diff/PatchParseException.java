package com.zutubi.diff;

/**
 * Exception raised on error parsing a unified diff into a patch file.
 *
 * @see PatchFile
 */
public class PatchParseException extends DiffException
{
    public PatchParseException()
    {
    }

    public PatchParseException(int line, String message)
    {
        super(Integer.toString(line) + ": " + message);
    }

    public PatchParseException(int line, String message, Throwable cause)
    {
        super(Integer.toString(line) + ": " + message, cause);
    }

    public PatchParseException(Throwable cause)
    {
        super(cause);
    }
}
