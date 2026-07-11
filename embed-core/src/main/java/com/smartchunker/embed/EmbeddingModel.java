package com.smartchunker.embed;

import com.smartchunker.embed.model.EmbeddingResult;

import java.util.List;

public interface EmbeddingModel {

    int dimension();

    EmbeddingResult embed(String text);

    List<EmbeddingResult> embedBatch(List<String> texts);
}