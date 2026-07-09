package com.smartchunker.engine;

import com.smartchunker.core.SmartChunker;
import com.smartchunker.core.ChunkerFactory;
import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import com.smartchunker.engine.model.BatchResult;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkEngine {

    private final SmartChunker chunker;
    private final ExecutorService executor;

    public ChunkEngine() {
        this.chunker = ChunkerFactory.createMarkdownChunker();
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    public ChunkEngine(int threadCount) {
        this.chunker = ChunkerFactory.createMarkdownChunker();
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public BatchResult processDirectory(File directory, ChunkConfig config) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("请传入目录: " + directory.getAbsolutePath());
        }

        File[] mdFiles = directory.listFiles(
                (dir, name) -> name.endsWith(".md")
        );

        if (mdFiles == null || mdFiles.length == 0) {
            return new BatchResult(Collections.emptyMap(), 0, 0);
        }

        long startTime = System.currentTimeMillis();
        Map<String, List<DocumentChunk>> resultMap = new ConcurrentHashMap<>();

        CompletableFuture<?>[] futures = new CompletableFuture[mdFiles.length];
        for (int i = 0; i < mdFiles.length; i++) {
            final File file = mdFiles[i];
            futures[i] = CompletableFuture.supplyAsync(() -> {
                List<DocumentChunk> chunks = chunker.process(file, config);
                resultMap.put(file.getName(), chunks);
                return chunks;
            }, executor);
        }

        CompletableFuture.allOf(futures).join();

        long elapsed = System.currentTimeMillis() - startTime;
        return new BatchResult(resultMap, elapsed, mdFiles.length);
    }

    public void shutdown() {
        executor.shutdown();
    }
}