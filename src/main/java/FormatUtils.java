import java.util.ArrayList;
import java.util.List;

public final class FormatUtils {

    private FormatUtils() {}

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("headers cannot be empty");
        }
        if (rows == null) rows = List.of();

        int cols = headers.length;
        List<String[]> safeRows = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            String[] r = new String[cols];
            for (int c = 0; c < cols; c++) {
                r[c] = (row != null && c < row.length && row[c] != null) ? row[c] : "";
            }
            safeRows.add(r);
        }

        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i] == null ? 0 : headers[i].length();
        }
        for (String[] row : safeRows) {
            for (int i = 0; i < cols; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder sb = new StringBuilder();
        String border = tableBorder(widths);
        sb.append(border).append('\n');
        sb.append(tableRow(headers, widths)).append('\n');
        sb.append(border).append('\n');
        for (String[] row : safeRows) {
            sb.append(tableRow(row, widths)).append('\n');
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatBox(String text) {
        if (text == null) text = "";
        String[] lines = text.replace("\r", "").split("\n", -1);
        int width = 0;
        for (String l : lines) width = Math.max(width, l.length());

        String top = "+" + "-".repeat(width + 2) + "+";
        StringBuilder sb = new StringBuilder();
        sb.append(top).append('\n');
        for (String l : lines) {
            sb.append("| ").append(padRight(l, width)).append(" |").append('\n');
        }
        sb.append(top);
        return sb.toString();
    }

    public static String formatHeader(String text) {
        if (text == null) text = "";
        String t = text.trim();
        String line = "=".repeat(Math.max(3, t.length() + 8));
        return line + "\n=== " + t + " ===\n" + line;
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength < 0) throw new IllegalArgumentException("maxLength must be >= 0");
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        String t = text == null ? "" : text;
        if (t.length() >= length) return t;
        return t + " ".repeat(length - t.length());
    }

    public static String padLeft(String text, int length) {
        String t = text == null ? "" : text;
        if (t.length() >= length) return t;
        return " ".repeat(length - t.length()) + t;
    }

    private static String tableBorder(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append('+');
        }
        return sb.toString();
    }

    private static String tableRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append('|');
        for (int i = 0; i < widths.length; i++) {
            String cell = cells[i] == null ? "" : cells[i];
            sb.append(' ').append(padRight(cell, widths[i])).append(' ').append('|');
        }
        return sb.toString();
    }
}

