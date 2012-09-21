package com.zutubi.diff.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Simple utilities for use with java.io.
 */
public class IOUtils
{
    private static final int BUFFER_SIZE = 8192;

    /**
     * Reads all bytes from the input stream and writes them to the output
     * stream.  Bytes are read and written in chunks.  The caller retains
     * ownership of the streams - i.e. they are not closed by this method.
     *
     * @param input  stream to read from
     * @param output stream to write to
     * @throws IOException on any error
     */
    public static void joinStreams(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        int n;

        while ((n = input.read(buffer)) > 0)
        {
            output.write(buffer, 0, n);
        }
    }

    /**
     * Closes a given closeable if it is not null, and ignores thrown errors.  Useful for finally
     * clauses which perform last-ditch cleanup and don't want to throw (masking earlier errors).
     *
     * @param closeable to be closed
     */
    public static void failsafeClose(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (IOException e)
            {
                // Ignored.
            }
        }
    }

    /**
     * Creates a file at the given path with the given content.  Uses the default locale.
     *
     * @param path path of the file to create
     * @param data the file contents
     * @throws IOException on any error
     */
    public static void createFile(File path, String data) throws IOException
    {
        OutputStreamWriter writer = null;
        try
        {
            writer = new OutputStreamWriter(new FileOutputStream(path));
            writer.write(data);
            writer.flush();
            writer.close();
        }
        finally
        {
            failsafeClose(writer);
        }
    }

    /**
     * Reads the entire contents of the file at the given path and returns it as a String.  Uses the default locale.
     *
     * @param path path of the file to read
     * @return the file content as a String
     * @throws IOException on any error
     */
    public static String readFile(File path) throws IOException
    {
        InputStreamReader reader = null;
        try
        {
            StringWriter writer = new StringWriter();
            reader = new InputStreamReader(new FileInputStream(path));

            int n;
            char[] buffer = new char[BUFFER_SIZE];
            while ((n = reader.read(buffer)) != -1)
            {
                writer.write(buffer, 0, n);
            }

            reader.close();
            return writer.toString();
        }
        finally
        {
            failsafeClose(reader);
        }
    }

    /**
     * Creates a directory at the given path, reporting errors as exceptions with more details
     * than {@link java.io.File#mkdir()}.
     *
     * @param path path of the directory to create
     * @throws IOException on any error
     */
    public static void createDirectory(File path) throws IOException
    {
        if (path.exists())
        {
            if (!path.isDirectory())
            {
                throw new IOException(String.format("Can not create directory. File '%s' already exists.", path));
            }
        }
        else if (!path.mkdirs())
        {
            throw new IOException(String.format("Failed to create directory '%s'", path));
        }
    }

    /**
     * Extracts the files from the given zip stream to into the given destination directory.
     *
     * @param zin zip stream to extract files from
     * @param dir destination directory
     * @throws IOException on error
     */
    public static void extractZip(ZipInputStream zin, File dir) throws IOException
    {
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null)
        {
            File file = new File(dir, entry.getName());

            if (entry.isDirectory())
            {
                if (!file.isDirectory())
                {
                    createDirectory(file);
                }
            }
            else
            {
                // Ensure that the file's parents already exist.
                if (!file.getParentFile().isDirectory())
                {
                    createDirectory(file.getParentFile());
                }

                unzip(zin, file);
            }

            file.setLastModified(entry.getTime());
        }
    }

    private static void unzip(InputStream zin, File file) throws IOException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(file);
            joinStreams(zin, out);
            out.close();
        }
        finally
        {
            failsafeClose(out);
        }
    }
}
