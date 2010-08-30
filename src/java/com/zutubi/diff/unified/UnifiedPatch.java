package com.zutubi.diff.unified;

import com.zutubi.diff.Patch;
import com.zutubi.diff.PatchApplyException;
import com.zutubi.diff.PatchType;
import com.zutubi.util.SystemUtils;
import com.zutubi.util.io.IOUtils;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a unified diff for a single file.  Patches are introduced by the:
 *
 * --- &lt;old file&gt;
 * +++ &lt;new file&gt;
 *
 * header in unified diffs.
 *
 * @see com.zutubi.diff.PatchFile
 */
public class UnifiedPatch implements Patch
{
    public static final String HEADER_OLD_FILE = "---";
    public static final String HEADER_NEW_FILE = "+++";
    public static final String NO_NEWLINE = "\\ No newline at end of file";

    private String oldFile;
    private boolean oldEpoch;
    private String newFile;
    private boolean newEpoch;
    private PatchType type;
    private boolean newlineTerminated = true;

    private List<UnifiedHunk> hunks = new LinkedList<UnifiedHunk>();
    private UnifiedHunk.Line lastLineInNew = null;

    /**
     * Create a new patch representing changes to a file.
     *
     * @param oldFile  the path of the old file that was changed
     * @param oldEpoch true iff the old file is marked with a timestamp of the
     *                 epoch (or equivalent)
     * @param newFile  the path of the new file that was created
     * @param newEpoch true iff the new file is marked with a timestamp of the
     *                 epoch (or equivalent)
     */
    public UnifiedPatch(String oldFile, boolean oldEpoch, String newFile, boolean newEpoch)
    {
        this.oldFile = oldFile;
        this.oldEpoch = oldEpoch;
        this.newFile = newFile;
        this.newEpoch = newEpoch;
    }

    public PatchType getType()
    {
        if (type == null)
        {
            if (isCreated())
            {
                type = PatchType.ADD;
            }
            else if (isRemoved())
            {
                type = PatchType.DELETE;
            }
            else
            {
                type = PatchType.EDIT;
            }
        }

        return type;
    }

    /**
     * @return true iff the old file did not exist - the file was created
     */
    public boolean isCreated()
    {
        if (oldEpoch)
        {
            for (UnifiedHunk hunk: hunks)
            {
                if (hunk.getOldLength() > 0)
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * @return true iff the new file does not exist - the file was removed
     */
    public boolean isRemoved()
    {
        if (newEpoch)
        {
            for (UnifiedHunk hunk: hunks)
            {
                if (hunk.getNewLength() > 0)
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public String getOldFile()
    {
        return oldFile;
    }

    public String getNewFile()
    {
        return newFile;
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
     * Flag whether or not the new file is terminated with a newline.
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
    public List<UnifiedHunk> getHunks()
    {
        return Collections.unmodifiableList(hunks);
    }

    /**
     * Append a hunk to this patch.
     *
     * @param hunk the hunk to append
     */
    void addHunk(UnifiedHunk hunk)
    {
        hunks.add(hunk);
        for (UnifiedHunk.Line line: hunk.getLines())
        {
            if (line.getType().inNew())
            {
                lastLineInNew = line;
            }
        }
    }

    public void apply(File file) throws PatchApplyException
    {
        if (isRemoved())
        {
            if (!file.delete())
            {
                throw new PatchApplyException("Unable to delete file '" + file.getAbsolutePath() + "'");
            }
        }
        else if (isCreated() || file.length() == 0)
        {
            if (isCreated() && file.exists())
            {
                throw new PatchApplyException("Cannot add file '" + file.getAbsolutePath() + "': file already exists");
            }

            FileWriter writer = null;
            try
            {
                writer = new FileWriter(file);
                for (UnifiedHunk hunk : hunks)
                {
                    for (UnifiedHunk.Line line : hunk.getLines())
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

    private void writeLine(FileWriter writer, UnifiedHunk.Line line) throws IOException
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
            Iterator<UnifiedHunk> hunkIt = hunks.iterator();
            UnifiedHunk hunk = null;
            Iterator<UnifiedHunk.Line> lineIt = null;
            UnifiedHunk nextHunk = hunkIt.hasNext() ? hunkIt.next() : null;

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
                    UnifiedHunk.Line hunkLine = lineIt.next();
                    UnifiedHunk.LineType changeType = hunkLine.getType();
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

        copyTempToTarget(file, tempFile);
        
        if (!tempFile.delete())
        {
            throw new PatchApplyException("Cannot remove temporary file '" + tempFile.getAbsolutePath() + "'");
        }
    }

    /**
     * Copies the temporary file to the target file.  This is done in this
     * manner to preserve the target file's permissions.
     * 
     * @param file     the target file
     * @param tempFile the temporary file
     * @throws PatchApplyException on an I/O error
     */
    private void copyTempToTarget(File file, File tempFile) throws PatchApplyException
    {
        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = new FileInputStream(tempFile);
            os = new FileOutputStream(file);
            IOUtils.joinStreams(is, os);
        }
        catch (IOException e)
        {
            throw new PatchApplyException("Cannot copy temporary file '" + tempFile.getAbsolutePath() + "' to original file '" + file.getAbsolutePath() + "'");
        }
        finally
        {
            IOUtils.close(is);
            IOUtils.close(os);
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

    private void applyTrailingHunkLines(File file, FileWriter writer, Iterator<UnifiedHunk.Line> lineIt) throws PatchApplyException, IOException
    {
        while (lineIt.hasNext())
        {
            UnifiedHunk.Line hunkLine = lineIt.next();
            if (hunkLine.getType().inOriginal())
            {
                throw new PatchApplyException("Patch does not apply cleanly: past end of original file '" + file.getAbsolutePath() + "': but found change of type '" + hunkLine.getType().name().toLowerCase() + "'");
            }

            if (hunkLine.getType().inNew())
            {
                writeLine(writer, hunkLine);
            }
        }
    }

    @Override
    public String toString()
    {
        return HEADER_OLD_FILE + " " + oldFile + '\t' + oldEpoch + '\n' + HEADER_NEW_FILE + " " + newFile + '\t' + newEpoch;
    }
}
