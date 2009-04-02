package com.zutubi.diff;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single change to a file as part of a larger patch.  Hunks
 * are introduced by the @@ ... @@ header in unified diffs.
 * 
 * @see Patch
 */
public class Hunk
{
    private long oldOffset;
    private long oldLength;
    private long newOffset;
    private long newLength;

    private List<Line> lines = new LinkedList<Line>();

    /**
     * Create a new hunk with the given locations in the old and new files.
     *
     * @param oldOffset one-based line offset of the first corresponding line
     *                  in the old file
     * @param oldLength number of lines in the hunk that appear in the old file
     * @param newOffset one-based line offset of the first corresponding line
     *                  in the new file
     * @param newLength number of lines in the hunk that appear in the new file
     */
    public Hunk(long oldOffset, long oldLength, long newOffset, long newLength)
    {
        this.oldOffset = oldOffset;
        this.oldLength = oldLength;
        this.newOffset = newOffset;
        this.newLength = newLength;
    }

    /**
     * @return one-based line offset of the first corresponding line in the old
     *         file
     */
    public long getOldOffset()
    {
        return oldOffset;
    }

    /**
     * @return number of lines in the hunk that appear in the old file
     */
    public long getOldLength()
    {
        return oldLength;
    }

    /**
     * @return one-based line offset of the first corresponding line in the new
     *         file
     */
    public long getNewOffset()
    {
        return newOffset;
    }

    /**
     * @return number of lines in the hunk that appear in the new file
     */
    public long getNewLength()
    {
        return newLength;
    }

    /**
     * @return the lines in the hunk, tagged with the type of change
     */
    public List<Line> getLines()
    {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Appends a line to this hunk.
     *
     * @param lineType type of change represented by the line
     * @param content  the line content
     */
    void addLine(LineType lineType, String content)
    {
        lines.add(new Line(lineType, content));
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        result.append("@@ -");
        result.append(oldOffset);
        result.append(',');
        result.append(oldLength);
        result.append(" +");
        result.append(newOffset);
        result.append(',');
        result.append(newLength);
        result.append(" @@\n");

        for (Line line: lines)
        {
            result.append(line.getType().getIndicator());
            result.append(line.getContent());
            result.append('\n');
        }

        return result.toString();
    }

    /**
     * Represents a single line change.
     */
    public static class Line
    {
        private LineType type;
        private String content;

        Line(LineType type, String content)
        {
            this.type = type;
            this.content = content;
        }

        public LineType getType()
        {
            return type;
        }

        public String getContent()
        {
            return content;
        }

        @Override
        public String toString()
        {
            return type.getIndicator() + content;
        }
    }

    /**
     * Types of lines in a hunk.
     */
    public enum LineType
    {
        /**
         * The line was added in the new file.
         */
        ADDED('+')
        {
            public boolean inOriginal()
            {
                return false;
            }

            public boolean inNew()
            {
                return true;
            }
        },
        /**
         * The line is common to both files.
         */
        COMMON(' ')
        {
            public boolean inOriginal()
            {
                return true;
            }

            public boolean inNew()
            {
                return true;
            }
        },
        /**
         * The line was deleted in the new file.
         */
        DELETED('-')
        {
            public boolean inOriginal()
            {
                return true;
            }

            public boolean inNew()
            {
                return false;
            }
        };

        private char indicator;

        private LineType(char indicator)
        {
            this.indicator = indicator;
        }

        /**
         * @return the character used to tag lines of this type in unified diff
         *         format
         */
        public char getIndicator()
        {
            return indicator;
        }

        /**
         * @return true iff this line appears in the original file
         */
        public abstract boolean inOriginal();

        /**
         * @return true iff this line appears in the new file
         */
        public abstract boolean inNew();

        /**
         * Converts a tag character from a unified diff to the corresponding
         * line type, if one exists.
         *
         * @param c the tag character to convert
         * @return type corresponding to the character, or null if the
         *         character matches no type
         */
        public static LineType valueOf(char c)
        {
            switch (c)
            {
                case '+':
                    return ADDED;
                case ' ':
                    return COMMON;
                case '-':
                    return DELETED;
                default:
                    return null;
            }
        }
    }
}
