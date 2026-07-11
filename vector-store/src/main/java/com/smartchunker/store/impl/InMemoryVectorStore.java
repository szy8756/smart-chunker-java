package com.smartchunker.store.impl;

import com.smartchunker.store.VectorStore;
import com.smartchunker.store.model.SearchHit;
import com.smartchunker.store.model.VectorDoc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryVectorStore implements VectorStore {

    private final Map<String, VectorDoc> docs = new ConcurrentHashMap<>();

    @Override
    public void add(String id, float[] vector, Map<String, String> metadata) {
        add(new VectorDoc(id, vector, metadata));
    }

    @Override
    public void add(VectorDoc doc) {
        docs.put(doc.getId(), doc);
    }

    @Override
    public List<SearchHit> search(float[] queryVector, int topK) {
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }

        PriorityQueue<SearchHit> heap = new PriorityQueue<>(
                Comparator.comparingDouble(SearchHit::getScore)
        );

        for (VectorDoc doc : docs.values()) {
            float similarity = cosineSimilarity(queryVector, doc.getVector());
            SearchHit hit = new SearchHit(doc.getId(), similarity, doc.getMetadata());

            if (heap.size() < topK) {
                heap.offer(hit);
            } else if (similarity > heap.peek().getScore()) {
                heap.poll();
                heap.offer(hit);
            }
        }

        List<SearchHit> results = new ArrayList<>(heap);
        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results;
    }

    @Override
    public void delete(String id) {
        docs.remove(id);
    }

    @Override
    public int size() {
        return docs.size();
    }

    @Override
    public void clear() {
        docs.clear();
    }

    @Override
    public List<VectorDoc> getAll() {
        return new ArrayList<>(docs.values());
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "向量维度不匹配: " + a.length + " vs " + b.length);
        }

        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}