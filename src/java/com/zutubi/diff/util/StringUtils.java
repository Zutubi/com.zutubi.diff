package com.zutubi.diff.util;

/**
 * Utility methods for working with strings.
 */
public class StringUtils
{
    /**
     * Returns the given string with the given prefix stripped iff the string
     * begins with the prefix.  Otherwise, the string is returned unchanged.
     *
     * @param s      the string to strip
     * @param prefix the candidate prefix to remove if present
     * @return the given string with the given prefix removed
     */
    public static String stripPrefix(String s, String prefix)
    {
        if (s != null && s.startsWith(prefix))
        {
            return s.substring(prefix.length());
        }
        else
        {
            return s;
        }
    }}
