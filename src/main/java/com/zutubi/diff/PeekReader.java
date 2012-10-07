package com.zutubi.diff;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * A wrapper around a buffered reader that allows peeking at the next line.
 */
public class PeekReader implements Closeable
{
    private BufferedReader delegate;
    private String nextLine;
    private int lineNumber;

    /**
     * Creates a new reader wrapping the given one.
     *
     * @param reader the reader to wrap, will be closed when this reader is
     *               closed
     * @throws IOException if an error occurs reading from the delegate
     */
    public PeekReader(Reader reader) throws IOException
    {
        delegate = new BufferedReader(reader);
        nextLine = delegate.readLine();
        lineNumber = 0;
    }

    /**
     * Peeks at the next line without consuming a line.
     *
     * @return the next line that would be returned by {@link #next()}
     */
    public String peek()
    {
        return nextLine;
    }

    /**
     * Consumes and returns a line of input.  The reader must not be spent.
     *
     * @return the next line of input
     * @throws IOException on error reading from the wrapped reader
     * @throws PatchParseException if the reader is already spent
     */
    public String next() throws IOException, PatchParseException
    {
        if (spent())
        {
            throw new PatchParseException(lineNumber, "Unexpected end of input");
        }

        String result = nextLine;
        nextLine = delegate.readLine();
        lineNumber++;
        return result;
    }

    /**
     * Indicates if this reader has consumed all available input.
     *
     * @return true if there is no more input
     */
    public boolean spent()
    {
        return nextLine == null;
    }

    /**
     * Returns the one-based number of the last line returned by
     * {@link #next()}.
     *
     * @return the current line number
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Closes this reader by closing the underlying reader.
     *
     * @throws IOException on error closing the underlying reader
     */
    public void close() throws IOException
    {
        delegate.close();
    }
}
