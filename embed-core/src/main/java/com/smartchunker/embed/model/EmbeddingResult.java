package com.smartchunker.embed.model;

import java.util.Arrays;

public class EmbeddingResult {

    private final String text;
    private final float[] vector;

    public EmbeddingResult(String text, float[] vector) {
        this.text = text;
        this.vector = vector;
    }

    public String getText() {
        return text;
    }

    public float[] getVector() {
        return vector;
    }

    public int dimension() {
        return vector == null ? 0 : vector.length;
    }

    @Override
    public String toString() {
        return "EmbeddingResult{text='" + text + "', dim=" + dimension() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbeddingResult)) return false;
        EmbeddingResult that = (EmbeddingResult) o;
        return Arrays.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }
}