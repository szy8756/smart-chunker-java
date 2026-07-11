package com.smartchunker.store.model;

import java.util.HashMap;
import java.util.Map;

public class VectorDoc {

    private final String id;
    private final float[] vector;
    private final Map<String, String> metadata;

    public VectorDoc(String id, float[] vector) {
        this(id, vector, new HashMap<>());
    }

    public VectorDoc(String id, float[] vector, Map<String, String> metadata) {
        this.id = id;
        this.vector = vector;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public float[] getVector() {
        return vector;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public int dimension() {
        return vector == null ? 0 : vector.length;
    }

    @Override
    public String toString() {
        return "VectorDoc{id='" + id + "', dim=" + dimension() + "}";
    }
}