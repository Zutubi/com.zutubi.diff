package com.zutubi.diff;

import java.io.IOException;

/**
 * Interface for parsers that can identify and parse patches.  Implementations
 * of this class can be used with {@link PatchFileParser} to parse a patch
 * file.
 */
public interface PatchParser
{
    /**
     * Tests if the given input line is the start of a patch that this parser
     * can handle.
     *
     * @param line the line to test
     * @return true if the given line begins a patch
     */
    boolean isPatchHeader(String line);

    /**
     * Parses a patch from the given reader.  The reader will be positioned at
     * the header line for a patch (i.e. isPatchHeader(reader.peek()) would
     * return true).  Parsing should not consume any lines beyond the end of
     * the recognised patch.
     *
     * @param reader reader to consume input from - should be left open
     * @return the parsed patch
     * @throws IOException if there is an error reading input
     * @throws PatchParseException if there is an error interpreting input
     */
    Patch parse(PeekReader reader) throws IOException, PatchParseException;
}
