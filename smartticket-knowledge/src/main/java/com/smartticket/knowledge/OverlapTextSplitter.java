package com.smartticket.knowledge;

import java.util.*;

public class OverlapTextSplitter {
    private final int chunkSize;
    private final int overlap;

    public OverlapTextSplitter(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public OverlapTextSplitter() { this(500, 100); }

    public List<Chunk> split(String text, Long docId) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        int start = 0, index = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            if (end < text.length()) end = adjustToSentenceBoundary(text, start, end);
            String content = text.substring(start, end).trim();
            if (!content.isBlank()) chunks.add(new Chunk(docId, index++, content));
            if (end >= text.length()) break;
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    private int adjustToSentenceBoundary(String text, int start, int end) {
        for (int i = end; i > start + chunkSize * 0.6; i--) {
            char c = text.charAt(i - 1);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == '.' || c == '!' || c == '?') return i;
        }
        return end;
    }

    public record Chunk(Long docId, int index, String content) {}
}
