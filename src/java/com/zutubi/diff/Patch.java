package com.zutubi.diff;

import com.zutubi.util.SystemUtils;
import com.zutubi.util.io.IOUtils;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a patch to a single file.  Compatible with patches in unified
 * diff format.  Patches are introduced by the:
 *
 * --- &lt;old file&gt;
 * +++ &lt;new file&gt;
 *
 * header in unified diffs.
 *
 * @see PatchFile
 */
public class Patch
{
    private String oldFile;
    private boolean isAdded;
    private String newFile;
    private boolean isDeleted;
    private boolean newlineTerminated = true;

    private List<Hunk> hunks = new LinkedList<Hunk>();
    private Hunk.Line lastLineInNew = null;

    /**
     * Create a new patch representing changes to a file.
     *
     * @param oldFile the path of the old file that was changed
     * @param added   true iff the old file did not exist - the file was added
     * @param newFile the path of the new file that was created
     * @param deleted true iff the new file does not exist - the file was
     *                deleted
     */
    public Patch(String oldFile, boolean added, String newFile, boolean deleted)
    {
        this.oldFile = oldFile;
        isAdded = added;
        this.newFile = newFile;
        isDeleted = deleted;
    }

    /**
     * @return the path of the old file that was changed
     */
    public String getOldFile()
    {
        return oldFile;
    }

    /**
     * @return true iff the old file did not exist - the file was added
     */
    public boolean isAdded()
    {
        return isAdded;
    }

    /**
     * @return the path of the new file that was created
     */
    public String getNewFile()
    {
        return newFile;
    }

    /**
     * @return true iff the new file does not exist - the file was deleted
     */
    public boolean isDeleted()
    {
        return isDeleted;
    }

    /**
     * Indicates whether the new file is terminated by a newline character.
     *
     * @return true iff the new file ends with a newline
     */
    public boolean isNewlineTerminated()
    {
        return newlineTerminated;
    }

    /**
     * Flag whether or not the new file is termianed with a newline.
     *
     * @param newlineTerminated set to true to indicate the new file ends with
     *
     */
    void setNewlineTerminated(boolean newlineTerminated)
    {
        this.newlineTerminated = newlineTerminated;
    }

    /**
     * Returns the list of hunks or changes in the patch.
     *
     * @return the list of hunks in the patch (unmodifiable)
     */
    public List<Hunk> getHunks()
    {
        return Collections.unmodifiableList(hunks);
    }

    /**
     * Append a hunk to this patch.
     *
     * @param hunk the hunk to append
     */
    void addHunk(Hunk hunk)
    {
        hunks.add(hunk);
        for (Hunk.Line line: hunk.getLines())
        {
            if (line.getType().inNew())
            {
                lastLineInNew = line;
            }
        }
    }

    /**
     * Apply this patch to the given file.  The changes represented by this
     * patch's hunks are applied in place to the file.  All common lines,
     * including context, must be found exactly where specified by the patch
     * -- i.e. no fuzzy patching is done.
     *
     * @param file the file to patch
     * @throws PatchApplyException if the patch does not apply cleanly (likely
     *         because the file differs from the old file in the diff) or an
     *         I/O error occurs
     */
    public void apply(File file) throws PatchApplyException
    {
        if (isDeleted)
        {
            if (!file.delete())
            {
                throw new PatchApplyException("Unable to delete file '" + file.getAbsolutePath() + "'");
            }
        }
        else if (isAdded || file.length() == 0)
        {
            if (isAdded && file.exists())
            {
                throw new PatchApplyException("Cannot add file '" + file.getAbsolutePath() + "': file already exists");
            }

            FileWriter writer = null;
            try
            {
                writer = new FileWriter(file);
                for (Hunk hunk : hunks)
                {
                    for (Hunk.Line line : hunk.getLines())
                    {
                        writeLine(writer, line);
                    }
                }
            }
            catch (IOException e)
            {
                throw new PatchApplyException("I/O error: " + e.getMessage(), e);
            }
            finally
            {
                IOUtils.close(writer);
            }
        }
        else
        {
            applyChanges(file);
        }
    }

    private void writeLine(FileWriter writer, Hunk.Line line) throws IOException
    {
        writer.write(line.getContent());
        if (newlineTerminated || line != lastLineInNew)
        {
            writer.write(SystemUtils.LINE_SEPARATOR);
        }
    }

    private void applyChanges(File file) throws PatchApplyException
    {
        File tempFile = chooseTempFile(file);
        BufferedReader reader = null;
        FileWriter writer = null;
        try
        {
            reader = new BufferedReader(new FileReader(file));
            writer = new FileWriter(tempFile);

            String line = reader.readLine();
            int lineNumber = 1;
            Iterator<Hunk> hunkIt = hunks.iterator();
            Hunk hunk = null;
            Iterator<Hunk.Line> lineIt = null;
            Hunk nextHunk = hunkIt.hasNext() ? hunkIt.next() : null;

            while (line != null)
            {
                if (hunk == null || !lineIt.hasNext())
                {
                    hunk = null;
                    lineIt = null;

                    // Should we start the next hunk?
                    if (nextHunk != null && nextHunk.getOldOffset() == lineNumber)
                    {
                        hunk = nextHunk;
                        lineIt = hunk.getLines().iterator();
                        nextHunk = hunkIt.hasNext() ? hunkIt.next() : null;
                    }
                }

                if (hunk == null)
                {
                    writer.write(line);
                    writer.write(SystemUtils.LINE_SEPARATOR);
                    lineNumber++;
                    line = reader.readLine();
                }
                else
                {
                    Hunk.Line hunkLine = lineIt.next();
                    Hunk.LineType changeType = hunkLine.getType();
                    if (changeType.inOriginal())
                    {
                        if (!line.equals(hunkLine.getContent()))
                        {
                            throw new PatchApplyException("Patch does not apply cleanly: original line " + lineNumber + " (hunk starting at original line " + hunk.getOldOffset() + ") of file '" + file.getAbsolutePath() + "'");
                        }

                        lineNumber++;
                        line = reader.readLine();
                    }

                    if (changeType.inNew())
                    {
                        writeLine(writer, hunkLine);
                    }
                }
            }

            // Apply lines and hunks from beyond the end of the original file.
            if (lineIt != null)
            {
                applyTrailingHunkLines(file, writer, lineIt);
            }
            
            while (hunkIt.hasNext())
            {
                applyTrailingHunkLines(file, writer, hunkIt.next().getLines().iterator());
            }
        }
        catch (IOException e)
        {
            throw new PatchApplyException("I/O error: " + e.getMessage(), e);
        }
        finally
        {
            IOUtils.close(reader);
            IOUtils.close(writer);
        }

        if (!file.delete())
        {
            throw new PatchApplyException("Cannot remove original file '" + file.getAbsolutePath() + "'");
        }

        if (!tempFile.renameTo(file))
        {
            throw new PatchApplyException("Cannot rename temp file '" + tempFile.getAbsolutePath() + "' to '" + file.getAbsolutePath() + "'");
        }
    }

    private File chooseTempFile(File file)
    {
        File candidate = new File(file.getAbsolutePath() + ".patch");
        while (candidate.exists())
        {
            candidate = new File(candidate.getAbsolutePath() + "x");
        }

        return candidate;
    }

    private void applyTrailingHunkLines(File file, FileWriter writer, Iterator<Hunk.Line> lineIt) throws PatchApplyException, IOException
    {
        while (lineIt.hasNext())
        {
            Hunk.Line hunkLine = lineIt.next();
            if (hunkLine.getType().inOriginal())
            {
                throw new PatchApplyException("Patch does not apply cleanly: past end of original file '" + file.getAbsolutePath() + "': but found change of type '" + hunkLine.getType().name().toLowerCase());
            }

            if (hunkLine.getType().inNew())
            {
                writeLine(writer, hunkLine);
            }
        }
    }
}
