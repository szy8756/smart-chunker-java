package com.smartchunker.starter;

import com.smartchunker.core.SmartChunker;
import com.smartchunker.core.ChunkerFactory;
import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import com.smartchunker.starter.config.ChunkerProperties;

import java.io.File;
import java.util.List;

public class SmartChunkerTemplate {

    private final SmartChunker chunker;
    private final ChunkerProperties properties;

    public SmartChunkerTemplate(ChunkerProperties properties) {
        this.chunker = ChunkerFactory.createMarkdownChunker();
        this.properties = properties;
    }

    public List<DocumentChunk> process(File file) {
        ChunkConfig config = buildConfig();
        return chunker.process(file, config);
    }

    public List<DocumentChunk> process(String markdown) {
        ChunkConfig config = buildConfig();
        return chunker.process(markdown, config);
    }

    private ChunkConfig buildConfig() {
        return new ChunkConfig(
                properties.getMaxChunkSize(),
                properties.getOverlapSize(),
                properties.isProtectCodeBlock(),
                properties.isIncludeContextPath()
        );
    }
}