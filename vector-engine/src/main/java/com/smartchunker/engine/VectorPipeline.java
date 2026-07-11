package com.smartchunker.engine;

import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import com.smartchunker.embed.EmbeddingModel;
import com.smartchunker.embed.EmbeddingFactory;
import com.smartchunker.embed.model.EmbeddingResult;
import com.smartchunker.engine.model.IndexResult;
import com.smartchunker.engine.model.SearchResult;
import com.smartchunker.store.VectorStore;
import com.smartchunker.store.VectorStoreFactory;
import com.smartchunker.store.model.SearchHit;

import java.io.File;
import java.util.*;

public class VectorPipeline {

    private final ChunkEngine chunkEngine;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public VectorPipeline() {
        this.chunkEngine = new ChunkEngine();
        this.embeddingModel = EmbeddingFactory.createDummyModel();
        this.vectorStore = VectorStoreFactory.createInMemoryStore();
    }

    public VectorPipeline(EmbeddingModel embeddingModel) {
        this.chunkEngine = new ChunkEngine();
        this.embeddingModel = embeddingModel;
        this.vectorStore = VectorStoreFactory.createInMemoryStore();
    }

    public VectorPipeline(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.chunkEngine = new ChunkEngine();
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    public IndexResult buildIndex(File directory, ChunkConfig config) {
        long startTime = System.currentTimeMillis();

        Map<String, List<DocumentChunk>> fileChunks = chunkEngine
                .processDirectory(directory, config)
                .getFileChunks();

        int totalChunks = 0;
        int totalVectors = 0;

        for (Map.Entry<String, List<DocumentChunk>> entry : fileChunks.entrySet()) {
            String fileName = entry.getKey();
            List<DocumentChunk> chunks = entry.getValue();

            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                String docId = fileName + "#" + i;

                EmbeddingResult result = embeddingModel.embed(chunk.getContent());

                Map<String, String> metadata = new HashMap<>();
                metadata.put("fileName", fileName);
                metadata.put("contextPath", chunk.getContextPath());
                metadata.put("startLine", String.valueOf(chunk.getStartLine()));
                metadata.put("endLine", String.valueOf(chunk.getEndLine()));
                metadata.put("content", chunk.getContent());

                vectorStore.add(docId, result.getVector(), metadata);
                totalVectors++;
            }
            totalChunks += chunks.size();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new IndexResult(fileChunks.size(), totalChunks, totalVectors, elapsed);
    }

    public List<SearchResult> search(String query, int topK) {
        EmbeddingResult queryEmbedding = embeddingModel.embed(query);

        List<SearchHit> hits = vectorStore.search(queryEmbedding.getVector(), topK);

        List<SearchResult> results = new ArrayList<>(hits.size());
        for (SearchHit hit : hits) {
            Map<String, String> meta = hit.getMetadata();
            String content = meta != null ? meta.get("content") : "";
            String contextPath = meta != null ? meta.get("contextPath") : "";

            results.add(new SearchResult(
                    hit.getId(),
                    hit.getScore(),
                    content,
                    contextPath,
                    meta
            ));
        }

        return results;
    }

    public int getVectorCount() {
        return vectorStore.size();
    }

    public void shutdown() {
        chunkEngine.shutdown();
    }
}