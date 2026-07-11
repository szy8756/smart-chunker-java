package com.smartchunker.embed.impl;

public class OnnxEmbeddingConfig {

    private String modelPath;
    private String vocabPath;
    private int maxSequenceLength = 512;
    private PoolingStrategy poolingStrategy = PoolingStrategy.MEAN;
    private boolean normalize = true;

    public enum PoolingStrategy {
        MEAN,
        CLS,
        MAX
    }

    public OnnxEmbeddingConfig() {
    }

    public OnnxEmbeddingConfig(String modelPath, String vocabPath) {
        this.modelPath = modelPath;
        this.vocabPath = vocabPath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public OnnxEmbeddingConfig setModelPath(String modelPath) {
        this.modelPath = modelPath;
        return this;
    }

    public String getVocabPath() {
        return vocabPath;
    }

    public OnnxEmbeddingConfig setVocabPath(String vocabPath) {
        this.vocabPath = vocabPath;
        return this;
    }

    public int getMaxSequenceLength() {
        return maxSequenceLength;
    }

    public OnnxEmbeddingConfig setMaxSequenceLength(int maxSequenceLength) {
        this.maxSequenceLength = maxSequenceLength;
        return this;
    }

    public PoolingStrategy getPoolingStrategy() {
        return poolingStrategy;
    }

    public OnnxEmbeddingConfig setPoolingStrategy(PoolingStrategy poolingStrategy) {
        this.poolingStrategy = poolingStrategy;
        return this;
    }

    public boolean isNormalize() {
        return normalize;
    }

    public OnnxEmbeddingConfig setNormalize(boolean normalize) {
        this.normalize = normalize;
        return this;
    }
}