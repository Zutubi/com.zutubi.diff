package com.zutubi.diff.examples.patch;

import com.zutubi.diff.PatchApplyException;
import com.zutubi.diff.PatchFile;
import com.zutubi.diff.PatchFileParser;
import com.zutubi.diff.PatchParseException;
import com.zutubi.diff.unified.UnifiedPatchParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * An emulation of the standard patch(1) utility based on Zutubi diff.  Only supports
 * "patch -p < patchfile" style execution for unified diffs.  Does not do fuzzy matching.
 */
public class Patch
{
    private static final String FLAG_PREFIX = "-p";

    /**
     * Entry point, not really interesting, just argument and error handling.
     *
     * @param argv command-line arguments
     */
    public static void main(final String[] argv)
    {
        if (argv.length != 1 || !argv[0].startsWith(FLAG_PREFIX))
        {
            usage();
        }

        final String prefixString = argv[0].substring(FLAG_PREFIX.length());
        try
        {
            final int stripPrefix = Integer.parseInt(prefixString);
            patch(new File("."), stripPrefix);
        }
        catch (NumberFormatException e)
        {
            System.err.println("Error: Invalid prefix '" + prefixString + "'");
            usage();
        }
        catch (PatchParseException e)
        {
            System.err.println("Could not parse patch: " + e.getMessage());
            System.exit(2);
        }
        catch (PatchApplyException e)
        {
            System.err.println("Could not apply patch: " + e.getMessage());
            System.exit(3);
        }
    }

    private static void usage()
    {
        System.err.println("Usage: java -jar patch.jar -p<num>");
        System.err.println("       (supply the patch file as standard input)");
        System.exit(1);
    }

    /**
     * The interesting part.  Parses a unified diff by reading standard input and applies it in the
     * given directory, stripping the given prefix from paths.
     *
     * @param dir         directory in which to apply the patch
     * @param stripPrefix the number of leading slashes to strip from paths (including the elements
     *                    that preceded them)
     * @throws PatchParseException if the patch cannot be parsed, including I/O errors reading it
     * @throws PatchApplyException if the patch does not cleanly apply, or there is an I/O error
     *                             applying it
     */
    private static void patch(final File dir, final int stripPrefix) throws PatchParseException, PatchApplyException
    {
        // For other formats, pass a different parser into this constructor.
        final PatchFileParser parser = new PatchFileParser(new UnifiedPatchParser());
        final PatchFile patchFile = parser.parse(new BufferedReader(new InputStreamReader(System.in)));
        patchFile.apply(dir, stripPrefix);
    }
}

