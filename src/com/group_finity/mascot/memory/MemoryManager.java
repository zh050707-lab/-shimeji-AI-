package com.group_finity.mascot.memory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A small file-backed conversation memory manager.
 *
 * Design goals:
 * - Keep dependencies minimal (no external JSON libs)
 * - Store messages as simple line-based JSON-ish records in conf/memory.json
 * - Provide add/get/clear APIs for easy integration
 */
public class MemoryManager {

    private final File file;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public MemoryManager() {
        this(new File("conf/memory.json"));
    }

    public MemoryManager(File file) {
        this.file = file;
        ensureExists();
    }

    private void ensureExists() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!file.exists()) {
                Files.write(file.toPath(), "[]".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            // swallow: caller can observe errors on read/write
            e.printStackTrace();
        }
    }

    /**
     * Append a new message to memory.
     * Role is typically "user" or "assistant".
     */
    public synchronized void addMessage(String role, String text) throws IOException {
        List<Message> all = readAll();
        all.add(new Message(new Date(), role, text));
        writeAll(all);
    }

    /**
     * Get up to `limit` most recent messages (most recent last).
     */
    public synchronized List<Message> getRecentMessages(int limit) throws IOException {
        List<Message> all = readAll();
        if (limit <= 0 || limit >= all.size()) return all;
        return all.subList(all.size() - limit, all.size());
    }

    /**
     * Clear memory (truncate file to empty array).
     */
    public synchronized void clearMemory() throws IOException {
        writeAll(new ArrayList<Message>());
    }

    private List<Message> readAll() throws IOException {
        if (!file.exists()) return new ArrayList<Message>();
        java.util.List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        String content = String.join("\n", lines).trim();
        if (content.isEmpty() || content.equals("[]")) return new ArrayList<Message>();

        // Very small, forgiving parser expecting an array of objects with keys: time,role,text
        List<Message> out = new ArrayList<>();
        // Split on "},{" to get approximate objects
        String trimmed = content;
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        // split between objects like "}, {" (allow whitespace/newlines)
        String[] parts = trimmed.split("\\}\\s*,\\s*\\{");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            // after splitting on '} , {' the braces may be missing; ensure each part is a valid object
            if (!part.startsWith("{")) part = "{" + part;
            if (!part.endsWith("}")) part = part + "}";
            String time = extractString(part, "time");
            String role = extractString(part, "role");
            String text = extractString(part, "text");
            Date d = new Date();
            try {
                if (time != null && !time.isEmpty()) {
                    d = dateFormat.parse(time);
                }
            } catch (Exception e) {
                // ignore, use now
            }
            out.add(new Message(d, role == null ? "" : role, text == null ? "" : text));
        }
        return out;
    }

    private void writeAll(List<Message> messages) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            sb.append("  {");
            sb.append("\"time\":\"").append(escape(m.getTimeString())).append("\", ");
            sb.append("\"role\":\"").append(escape(m.role)).append("\", ");
            sb.append("\"text\":\"").append(escape(m.text)).append("\"");
            sb.append("}");
            if (i < messages.size() - 1) sb.append(",\n"); else sb.append('\n');
        }
        sb.append("]");

        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String extractString(String obj, String key) {
        // Find the key (e.g. "time") then locate the ':' and the opening quote for the value.
        int idx = obj.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        idx = obj.indexOf(':', idx);
        if (idx < 0) return null;
        idx++;
        // skip whitespace
        while (idx < obj.length() && Character.isWhitespace(obj.charAt(idx))) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '"') return null;
        idx++; // move past opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < obj.length()) {
            char c = obj.charAt(idx++);
            if (c == '"') break;
            if (c == '\\' && idx < obj.length()) {
                char n = obj.charAt(idx++);
                sb.append(n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static class Message {
        private final Date time;
        private final String role;
        private final String text;

        public Message(Date time, String role, String text) {
            this.time = time == null ? new Date() : time;
            this.role = role == null ? "" : role;
            this.text = text == null ? "" : text;
        }

        public Date getTime() { return time; }

        public String getRole() { return role; }

        public String getText() { return text; }

        public String getTimeString() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(time);
        }

        @Override
        public String toString() {
            return "[" + getTimeString() + "] " + role + ": " + text;
        }
    }
}
