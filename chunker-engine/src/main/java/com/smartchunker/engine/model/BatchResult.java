package com.smartchunker.engine.model;

import com.smartchunker.core.model.DocumentChunk;

import java.util.List;
import java.util.Map;

public class BatchResult {

    private final Map<String, List<DocumentChunk>> fileChunks;
    private final long elapsedMs;
    private final int fileCount;

    public BatchResult(Map<String, List<DocumentChunk>> fileChunks, long elapsedMs, int fileCount) {
        this.fileChunks = fileChunks;
        this.elapsedMs = elapsedMs;
        this.fileCount = fileCount;
    }

    public Map<String, List<DocumentChunk>> getFileChunks() {
        return fileChunks;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getTotalChunkCount() {
        return fileChunks.values().stream().mapToInt(List::size).sum();
    }
}