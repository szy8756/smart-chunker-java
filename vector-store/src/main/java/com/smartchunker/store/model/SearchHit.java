package com.smartchunker.store.model;

import java.util.Map;

public class SearchHit {

    private final String id;
    private final float score;
    private final Map<String, String> metadata;

    public SearchHit(String id, float score, Map<String, String> metadata) {
        this.id = id;
        this.score = score;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "SearchHit{id='" + id + "', score=" + String.format("%.4f", score) + "}";
    }
}