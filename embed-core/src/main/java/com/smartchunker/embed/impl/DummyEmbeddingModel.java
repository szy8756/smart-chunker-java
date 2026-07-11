package com.smartchunker.embed.impl;

import com.smartchunker.embed.EmbeddingModel;
import com.smartchunker.embed.model.EmbeddingResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DummyEmbeddingModel implements EmbeddingModel {

    private static final int DEFAULT_DIMENSION = 384;
    private final int dimension;

    public DummyEmbeddingModel() {
        this(DEFAULT_DIMENSION);
    }

    public DummyEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public EmbeddingResult embed(String text) {
        float[] vector = hashToVector(text);
        return new EmbeddingResult(text, vector);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        List<EmbeddingResult> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    private float[] hashToVector(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isEmpty()) {
            return vector;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));

            for (int i = 0; i < dimension; i++) {
                int byteIndex = i % hash.length;
                int value = hash[byteIndex] & 0xFF;
                vector[i] = (value - 127.5f) / 127.5f;
            }
        } catch (NoSuchAlgorithmException e) {
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) Math.sin(text.hashCode() * (i + 1) * 0.01);
            }
        }

        return normalize(vector);
    }

    private float[] normalize(float[] vector) {
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}