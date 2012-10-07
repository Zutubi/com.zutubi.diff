package com.zutubi.diff;

import junit.framework.TestCase;

import java.io.InputStream;

/**
 * Common base class for tests, with helpers.
 */
public abstract class DiffTestCase extends TestCase
{
    /**
     * Returns an input stream for test data located on the classpath, with the
     * given name and extension.  The test data is located using {@link Class#getResourceAsStream(String)},
     * with the name passed being &lt;simple classname&gt;.name.extension.  The
     * simplest way to use this method is to keep your test data file alongside
     * your test class and ensure it is "compiled" with the class to the
     * classpath.
     *
     * @see Class#getResourceAsStream(String)
     *
     * @param name      the name of the test data file
     * @param extension the extension of the test data file
     * @return an input stream open at the beginning of the test data
     */
    public InputStream getInput(String name, String extension)
    {
        String fullName = getClass().getSimpleName() + "." + name + "." + extension;
        InputStream stream = getClass().getResourceAsStream(fullName);
        if (stream == null)
        {
            fail("Required input '" + fullName + "' not found");
        }
        return stream;
    }
}
