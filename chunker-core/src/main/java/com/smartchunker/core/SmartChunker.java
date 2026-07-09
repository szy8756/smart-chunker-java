package com.smartchunker.core;

import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;

import java.io.File;
import java.util.List;

public interface SmartChunker {
    List<DocumentChunk> process(File file, ChunkConfig config);
    List<DocumentChunk> process(String markdownContent, ChunkConfig config);
}
