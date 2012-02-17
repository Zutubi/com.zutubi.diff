package com.zutubi.diff;

import com.zutubi.diff.unified.UnifiedPatch;
import com.zutubi.diff.unified.UnifiedPatchParser;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.SystemUtils;
import com.zutubi.util.ZipUtils;
import com.zutubi.util.junit.ZutubiTestCase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

/**
 * NOTE: cygwin is required on the path for windows machines.
 */
public class PatchFileParserTest extends ZutubiTestCase
{
    private static final String PREFIX_TEST = "test";
    private static final String EXTENSION_PATCH = "txt";

    private File tempDir;
    private File oldDir;
    private File newDir;
    private PatchFileParser parser;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        tempDir = createTempDirectory();
        oldDir = new File(tempDir, "old");
        newDir = new File(tempDir, "new");

        ZipUtils.extractZip(new ZipInputStream(getInput("old", "zip")), oldDir);
        ZipUtils.extractZip(new ZipInputStream(getInput("new", "zip")), newDir);

        parser = new PatchFileParser(new UnifiedPatchParser());
    }

    @Override
    protected void tearDown() throws Exception
    {
        removeDirectory(tempDir);
        super.tearDown();
    }

    public void testEdited() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEditedSingleLine() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEditedFirstLine() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEditedSecondLine() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEditedSecondLastLine() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEditedLastLine() throws Exception
    {
        readApplyAndCheck();
    }

    public void testLinesAdded() throws Exception
    {
        readApplyAndCheck();
    }

    public void testFirstLineAdded() throws Exception
    {
        readApplyAndCheck();
    }

    public void testLastLineAdded() throws Exception
    {
        readApplyAndCheck();
    }

    public void testLinesDeleted() throws Exception
    {
        readApplyAndCheck();
    }

    public void testFirstLineDeleted() throws Exception
    {
        readApplyAndCheck();
    }

    public void testLastLineDeleted() throws Exception
    {
        readApplyAndCheck();
    }

    public void testEmptied() throws Exception
    {
        readApplyAndCheck();
    }

    public void testUnemptied() throws Exception
    {
        readApplyAndCheck();
    }

    public void testVariousChanges() throws Exception
    {
        readApplyAndCheck();
    }

    public void testAddNewline() throws Exception
    {
        readApplyAndCheck();
    }

    public void testDeleteNewline() throws Exception
    {
        readApplyAndCheck();
    }

    public void testDeleted() throws Exception
    {
        readApplyAndCheck();
    }

    public void testNestedFile() throws Exception
    {
        readApplyAndCheck("nested/file-nested.txt");
    }

    public void testSpaceCharactersInFilename() throws Exception
    {
        readApplyAndCheck("file with space characters.txt");
    }

    public void testSubversionFileEdited() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileEditedFirstLine() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileEditedLastLine() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileEditedSecondLine() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileEditedSecondLastLine() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileEditedSingleLine() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileNested() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileVariousChanges() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileWithSpaceCharacters() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileLinesAdded() throws Exception
    {
        singleFilePatchHelper();
    }
    
    public void testSubversionFileLinesDeleted() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileFirstLineAdded() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileFirstLineDeleted() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileLastLineAdded() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileLastLineDeleted() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileAdded() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileAddNewline() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionFileDeleteNewline() throws Exception
    {
        singleFilePatchHelper();
    }

    public void testSubversionMergeinfo() throws Exception
    {
        PatchFile pf = parseSinglePatch();
        assertEquals(1, pf.getPatches().size());
        Patch patch = pf.getPatches().get(0);
        String patchedFile = patch.getNewFile();
        assertEquals(".", patchedFile);
        assertEquals(0, ((UnifiedPatch) patch).getHunks().size());
    }

    public void testSubversionDiffPlusMergeinfo() throws Exception
    {
        PatchFile pf = parseSinglePatch();
        assertEquals(2, pf.getPatches().size());

        Patch patch = pf.getPatches().get(0);
        assertEquals(".", patch.getNewFile());
        assertEquals(0, ((UnifiedPatch) patch).getHunks().size());

        patch = pf.getPatches().get(1);
        assertEquals("build.xml", patch.getNewFile());
        assertEquals(1, ((UnifiedPatch) patch).getHunks().size());
    }

    public void testBadOldFile() throws Exception
    {
        try
        {
            parseSinglePatch();
            fail("Input file shouldn't parse");
        }
        catch (PatchParseException e)
        {
            assertEquals(e.getMessage(), "1: Patch header line '---' is missing filename");
        }
    }

    private void readApplyAndCheck() throws Exception
    {
        readApplyAndCheck(convertName());
    }

    private void readApplyAndCheck(String name) throws DiffException, IOException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("diff", "-uN", "old/" + name, "new/" + name);
        processBuilder.directory(tempDir);
        String diff = SystemUtils.runCommandWithInput(1, null, processBuilder);

        File patch = new File(tempDir, "patch");
        FileSystemUtils.createFile(patch, diff);

        PatchFile patchFile = parser.parse(new FileReader(patch));
        applyAndCheck(patchFile, 1, name);
    }

    private void singleFilePatchHelper() throws PatchParseException, PatchApplyException, IOException
    {
        PatchFile pf = parseSinglePatch();
        assertEquals(1, pf.getPatches().size());
        String patchedFile = pf.getPatches().get(0).getNewFile();
        applyAndCheck(pf, 0, patchedFile);
    }

    private PatchFile parseSinglePatch() throws PatchParseException
    {
        return parser.parse(new InputStreamReader(getInput(EXTENSION_PATCH)));
    }

    private void applyAndCheck(PatchFile patchFile, int prefixStripCount, String name) throws PatchParseException, PatchApplyException, IOException
    {
        File in = new File(oldDir, name);
        boolean existedBefore = in.exists();

        patchFile.apply(oldDir, prefixStripCount);

        File out = new File(newDir, name);
        if (out.exists())
        {
            if (!existedBefore)
            {
                assertTrue("Expected file '" + in.getName() + "' to be added, but it does not exist", in.exists());
            }

            if (SystemUtils.IS_WINDOWS)
            {
                FileSystemUtils.translateEOLs(out, SystemUtils.CRLF_BYTES, true);
            }

            ProcessBuilder processBuilder = new ProcessBuilder("diff", "-uN", "old/" + name, "new/" + name);
            processBuilder.directory(tempDir);
            String diff = SystemUtils.runCommandWithInput(-1, null, processBuilder);
            assertTrue("Expected no differences, got:\n" + diff, diff.length() == 0);
        }
        else
        {
            assertFalse("Expected file '" + in.getName() + "' to be deleted, but it still exists", in.exists());
        }
    }

    private String convertName()
    {
        String name = getName();
        if (name.startsWith(PREFIX_TEST))
        {
            name = name.substring(PREFIX_TEST.length());
        }

        String convertedName = "";
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (Character.isUpperCase(c))
            {
                convertedName += "-";
            }
            convertedName += Character.toLowerCase(c);
        }

        return "file" + convertedName + ".txt";
    }

}
