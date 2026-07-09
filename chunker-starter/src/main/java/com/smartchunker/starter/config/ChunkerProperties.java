package com.smartchunker.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-chunker.strategy")
public class ChunkerProperties {

    private int maxChunkSize = 800;
    private int overlapSize = 100;
    private boolean protectCodeBlock = true;
    private boolean includeContextPath = true;

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    public void setOverlapSize(int overlapSize) {
        this.overlapSize = overlapSize;
    }

    public boolean isProtectCodeBlock() {
        return protectCodeBlock;
    }

    public void setProtectCodeBlock(boolean protectCodeBlock) {
        this.protectCodeBlock = protectCodeBlock;
    }

    public boolean isIncludeContextPath() {
        return includeContextPath;
    }

    public void setIncludeContextPath(boolean includeContextPath) {
        this.includeContextPath = includeContextPath;
    }
}