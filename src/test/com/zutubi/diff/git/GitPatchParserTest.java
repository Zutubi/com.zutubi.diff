package com.zutubi.diff.git;

import com.zutubi.diff.*;
import com.zutubi.diff.unified.UnifiedHunk;
import com.zutubi.util.junit.ZutubiTestCase;
import junit.framework.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class GitPatchParserTest extends ZutubiTestCase
{
    private static final String EXTENSION_TXT = "txt";

    private PatchFileParser parser = new PatchFileParser(new GitPatchParser());

    public void testSimpleEdit() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "README.txt", "README.txt", PatchType.EDIT);

        List<UnifiedHunk> hunks = ((GitUnifiedPatch) patch).getHunks();
        Assert.assertEquals(1, hunks.size());

        UnifiedHunk hunk = hunks.get(0);
        Assert.assertEquals(1, hunk.getOldOffset());
        Assert.assertEquals(6, hunk.getOldLength());
        
        List<UnifiedHunk.Line> lines = hunk.getLines();
        Assert.assertEquals(8, lines.size());
        UnifiedHunk.Line line = lines.get(3);
        Assert.assertEquals("Here is an edit.", line.getContent());
        Assert.assertEquals(UnifiedHunk.LineType.ADDED, line.getType());
    }

    public void testSimpleAdd() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "launchers/newfile.txt", "launchers/newfile.txt", PatchType.ADD);

        List<UnifiedHunk> hunks = ((GitUnifiedPatch) patch).getHunks();
        Assert.assertEquals(1, hunks.size());

        UnifiedHunk hunk = hunks.get(0);
        List<UnifiedHunk.Line> lines = hunk.getLines();
        Assert.assertEquals(2, lines.size());
        UnifiedHunk.Line line = lines.get(0);
        Assert.assertEquals("this is a", line.getContent());
        Assert.assertEquals(UnifiedHunk.LineType.ADDED, line.getType());
    }

    public void testSimpleDelete() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "clojurescript/MANIFEST.MF", "clojurescript/MANIFEST.MF", PatchType.DELETE);
    }

    public void testSimpleRename() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "src/clojure/contrib/apply_macro.clj", "src/clojure/contrib/apply_micro.clj", PatchType.RENAME);
    }

    public void testSimpleCopy() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "src/clojure/contrib/apply_macro.clj", "src/clojure/contrib/apply_micro.clj", PatchType.COPY);
    }

    public void testPureModeChange() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "pom.xml", "pom.xml", PatchType.METADATA);
    }

    public void testRenameWithEdit() throws PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "src/clojure/contrib/apply_macro.clj", "src/clojure/contrib/apply_micro.clj", PatchType.RENAME);

        List<UnifiedHunk> hunks = ((GitUnifiedPatch) patch).getHunks();
        Assert.assertEquals(1, hunks.size());
    }

    public void testBinaryAdd() throws PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "binfile", "binfile", PatchType.ADD);
        Assert.assertTrue(patch instanceof GitBinaryPatch);
    }

    public void testBinaryEdit() throws PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "binfile", "binfile", PatchType.EDIT);
        Assert.assertTrue(patch instanceof GitBinaryPatch);
    }

    public void testBinaryDelta() throws PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "binfile", "binfile", PatchType.EDIT);
        Assert.assertTrue(patch instanceof GitBinaryPatch);
    }

    public void testBinaryModeChange() throws IOException, PatchParseException
    {
        GitPatch patch = parseSinglePatch();
        assertPatchDetails(patch, "binfile", "binfile", PatchType.METADATA);
    }

    public void testVariousChanges() throws PatchParseException
    {
        PatchFile patchFile = parsePatchFile();
        List<Patch> patches = patchFile.getPatches();
        Assert.assertEquals(9, patches.size());
        assertPatchDetails(patches.get(0), ".gitignore", ".gitignore", PatchType.DELETE);
        assertPatchDetails(patches.get(1), "binfile", "binfile", PatchType.EDIT);
        assertPatchDetails(patches.get(2), "src/clojure/contrib/bin.clj", "src/clojure/contrib/bin.clj", PatchType.ADD);
        assertPatchDetails(patches.get(3), "src/clojure/contrib/classpath.clj", "src/clojure/contrib/classy.clj", PatchType.RENAME);
        assertPatchDetails(patches.get(4), "src/clojure/contrib/miglayout.clj", "src/clojure/contrib/miglayout.clj", PatchType.EDIT);
        assertPatchDetails(patches.get(5), "src/clojure/contrib/monads.clj", "src/clojure/contrib/monads.clj", PatchType.DELETE);
        assertPatchDetails(patches.get(6), "src/clojure/contrib/new.clj", "src/clojure/contrib/new.clj", PatchType.ADD);
        assertPatchDetails(patches.get(7), "src/clojure/contrib/xul.clj", "src/clojure/contrib/xul.clj", PatchType.ADD);
        assertPatchDetails(patches.get(8), "src/clojure/contrib/zip_filter.clj", "src/clojure/contrib/zip_filter.clj", PatchType.EDIT);
    }

    private void assertPatchDetails(Patch patch, String oldFile, String newFile, PatchType type)
    {
        Assert.assertEquals(oldFile, patch.getOldFile());
        Assert.assertEquals(newFile, patch.getNewFile());
        Assert.assertEquals(type, patch.getType());
    }

    private GitPatch parseSinglePatch() throws PatchParseException
    {
        PatchFile patchFile = parsePatchFile();
        List<Patch> patches = patchFile.getPatches();
        Assert.assertEquals(1, patches.size());
        return (GitPatch) patches.get(0);
    }

    private PatchFile parsePatchFile() throws PatchParseException
    {
        InputStream inputStream = getInput(EXTENSION_TXT);
        Reader reader = new InputStreamReader(inputStream);
        return parser.parse(reader);
    }
}

