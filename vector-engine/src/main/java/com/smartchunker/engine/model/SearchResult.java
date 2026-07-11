package com.smartchunker.engine.model;

import java.util.Map;

public class SearchResult {

    private final String id;
    private final float score;
    private final String content;
    private final String contextPath;
    private final Map<String, String> metadata;

    public SearchResult(String id, float score, String content, String contextPath,
                        Map<String, String> metadata) {
        this.id = id;
        this.score = score;
        this.content = content;
        this.contextPath = contextPath;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    public String getContent() {
        return content;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "SearchResult{id='" + id + "', score=" + String.format("%.4f", score)
                + ", contextPath='" + contextPath + "'}";
    }
}