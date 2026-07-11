package com.smartchunker.store;

import com.smartchunker.store.model.SearchHit;
import com.smartchunker.store.model.VectorDoc;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void add(String id, float[] vector, Map<String, String> metadata);

    void add(VectorDoc doc);

    List<SearchHit> search(float[] queryVector, int topK);

    void delete(String id);

    int size();

    void clear();

    List<VectorDoc> getAll();
}