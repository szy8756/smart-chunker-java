package com.smartchunker.engine.model;

public class IndexResult {

    private final int fileCount;
    private final int chunkCount;
    private final int vectorCount;
    private final long elapsedMs;

    public IndexResult(int fileCount, int chunkCount, int vectorCount, long elapsedMs) {
        this.fileCount = fileCount;
        this.chunkCount = chunkCount;
        this.vectorCount = vectorCount;
        this.elapsedMs = elapsedMs;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getVectorCount() {
        return vectorCount;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    @Override
    public String toString() {
        return "IndexResult{files=" + fileCount + ", chunks=" + chunkCount
                + ", vectors=" + vectorCount + ", elapsed=" + elapsedMs + "ms}";
    }
}