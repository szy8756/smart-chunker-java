package com.smartchunker.embed;

import com.smartchunker.embed.impl.DummyEmbeddingModel;

public class EmbeddingFactory {

    public static EmbeddingModel createDummyModel() {
        return new DummyEmbeddingModel();
    }

    public static EmbeddingModel createDummyModel(int dimension) {
        return new DummyEmbeddingModel(dimension);
    }
}