package com.zutubi.diff.util;

/**
 * Defines constants that depend on the host environment.
 */
public class Environment
{
    /**
     * True iff the host OS is some variant of Windows.
     */
    public static final boolean OS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");
    /**
     * Text file line separator for the host environment.
     */
    public static final String LINE_SEPARATOR;
    static
    {
        String sep = System.getProperty("line.separator");
        if (sep == null)
        {
            if (OS_WINDOWS)
            {
                sep = "\r\n";
            }
            else
            {
                sep = "\n";
            }
        }

        LINE_SEPARATOR = sep;
    }
}
