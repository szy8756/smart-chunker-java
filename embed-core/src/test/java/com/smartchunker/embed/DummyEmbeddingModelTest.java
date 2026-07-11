package com.smartchunker.embed;

import com.smartchunker.embed.model.EmbeddingResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DummyEmbeddingModelTest {

    @Test
    public void testDimension() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        assertEquals(384, model.dimension());

        EmbeddingModel customModel = EmbeddingFactory.createDummyModel(512);
        assertEquals(512, customModel.dimension());
    }

    @Test
    public void testSingleEmbed() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        EmbeddingResult result = model.embed("Hello World");

        assertNotNull(result);
        assertEquals("Hello World", result.getText());
        assertEquals(384, result.dimension());
        assertNotNull(result.getVector());
    }

    @Test
    public void testEmbedNormalized() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        EmbeddingResult result = model.embed("test text");

        float[] vector = result.getVector();
        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        float norm = (float) Math.sqrt(sum);
        assertEquals("向量应被归一化", 1.0f, norm, 0.0001f);
    }

    @Test
    public void testEmbedBatch() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        List<String> texts = Arrays.asList("apple", "banana", "cherry");

        List<EmbeddingResult> results = model.embedBatch(texts);

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    public void testEmbedConsistency() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        EmbeddingResult r1 = model.embed("hello");
        EmbeddingResult r2 = model.embed("hello");
        assertArrayEquals("相同文本应产生相同向量", r1.getVector(), r2.getVector(), 0.0001f);
    }

    @Test
    public void testEmptyText() {
        EmbeddingModel model = EmbeddingFactory.createDummyModel();
        EmbeddingResult result = model.embed("");
        assertNotNull(result);
        assertEquals(384, result.dimension());
    }
}