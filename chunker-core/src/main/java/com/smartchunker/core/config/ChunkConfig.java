package com.smartchunker.core.config;

public class ChunkConfig {

    private final int maxChunkSize; // 最大分块大小（字节）
    private final int overlapSize; // 分块重叠大小（字节）
    private final boolean protectCodeBlock; // 是否保护代码块不被分块
    private final boolean includeContextPath; // 是否包含上下文路径

    public ChunkConfig(int maxChunkSize, int overlapSize) {
        this(maxChunkSize, overlapSize, true, true);
    }

    public ChunkConfig(int maxChunkSize, int overlapSize, boolean protectCodeBlock, boolean includeContextPath) {
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
        this.protectCodeBlock = protectCodeBlock;
        this.includeContextPath = includeContextPath;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    public boolean isProtectCodeBlock() {
        return protectCodeBlock;
    }

    public boolean isIncludeContextPath() {
        return includeContextPath;
    }
}
