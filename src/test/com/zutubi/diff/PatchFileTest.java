package com.zutubi.diff;

import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.SystemUtils;
import com.zutubi.util.ZipUtils;
import com.zutubi.util.junit.ZutubiTestCase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class PatchFileTest extends ZutubiTestCase
{
    private static final String PREFIX_TEST = "test";

    private File tempDir;
    private File oldDir;
    private File newDir;

    public boolean isDiffPresent()
    {
        return !SystemUtils.IS_WINDOWS && SystemUtils.findInPath("diff") != null;
    }

    protected void runTest() throws Throwable
    {
        if (isDiffPresent())
        {
            super.runTest();
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        tempDir = FileSystemUtils.createTempDir(getName(), ".tmp");
        oldDir = new File(tempDir, "old");
        newDir = new File(tempDir, "new");

        ZipUtils.extractZip(new ZipInputStream(getInput("old", "zip")), oldDir);
        ZipUtils.extractZip(new ZipInputStream(getInput("new", "zip")), newDir);
    }

    @Override
    protected void tearDown() throws Exception
    {
        FileSystemUtils.rmdir(tempDir);
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

    public void testSpecialCharactersInFilename() throws Exception
    {
        readApplyAndCheck("file with \" odd characters.txt");
    }

    private void readApplyAndCheck() throws Exception
    {
        readApplyAndCheck(convertName());
    }

    private void readApplyAndCheck(String name) throws DiffException, IOException
    {
        File in = new File(oldDir, name);
        boolean existedBefore = in.exists();

        ProcessBuilder processBuilder = new ProcessBuilder("diff", "-uaN", "old/" + name, "new/" + name);
        processBuilder.directory(tempDir);
        String diff = SystemUtils.runCommandWithInput(1, null, processBuilder);

        File patch = new File(tempDir, "patch");
        FileSystemUtils.createFile(patch, diff);

        PatchFile patchFile = PatchFile.read(new FileReader(patch));
        patchFile.apply(oldDir, 1);

        File out = new File(newDir, name);

        if (out.exists())
        {
            if (existedBefore)
            {
                diff = SystemUtils.runCommandWithInput(-1, null, processBuilder);
                assertTrue("Expected no differences, got:\n" + diff, diff.length() == 0);
            }
            else
            {
                assertTrue("Expected file '" + in.getName() + "' to be added, but it does not exist", in.exists());
            }
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
