package com.zutubi.diff;

import com.zutubi.util.io.IOUtils;

import java.io.IOException;
import java.io.Reader;

/**
 * A generic patch file parser that can be used to parse files which are a
 * collection of patches.  Delegates to a {@link PatchParser} to do the actual
 * work of parsing the individual patches.
 */
public class PatchFileParser
{
    private PatchParser patchParser;

    /**
     * Create a parser that will use the given patch parser to extract patches
     * from a file.
     *
     * @param patchParser parser used to identify and read each patch
     */
    public PatchFileParser(PatchParser patchParser)
    {
        this.patchParser = patchParser;
    }

    /**
     * Parses a patch file from the given input reader.  The input patch file
     * should contain one or more patches in the format expected by our patch
     * parser.  The parsing is permissive - ignoring unexpected lines where
     * possible.
     *
     * @param input input to read the patch content from - ownership is taken
     *              and this method will close the reader
     * @return the parsed patch file
     * @throws PatchParseException on an unrecoverable parse or I/O error
     */
    public PatchFile parse(Reader input) throws PatchParseException
    {
        PeekReader peekReader = null;
        try
        {
            peekReader = new PeekReader(input);
            return read(peekReader, patchParser);
        }
        catch (IOException e)
        {
            throw new PatchParseException(-1, "I/O error reading patch: " + e.getMessage(), e);
        }
        finally
        {
            IOUtils.close(peekReader);
        }
    }

    private PatchFile read(PeekReader reader, PatchParser parser) throws IOException, PatchParseException
    {
        PatchFile patchFile = new PatchFile();
        while (!reader.spent())
        {
            // Look for the patch header, skipping anything extra that may
            // be in the way.
            String line = reader.peek();
            if (parser.isPatchHeader(line))
            {
                patchFile.addPatch(parser.parse(reader));
            }
            else
            {
                reader.next();
            }
        }

        return patchFile;
    }
}
