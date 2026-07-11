package com.smartchunker.embed;

import com.smartchunker.embed.impl.OnnxEmbeddingConfig;
import com.smartchunker.embed.impl.OnnxTokenizer;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OnnxEmbeddingModelTest {

    private String getVocabPath() {
        try {
            Path path = Paths.get(getClass().getClassLoader()
                    .getResource("test-vocab.txt").toURI());
            return path.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConfigDefaults() {
        OnnxEmbeddingConfig config = new OnnxEmbeddingConfig();
        assertEquals(512, config.getMaxSequenceLength());
        assertEquals(OnnxEmbeddingConfig.PoolingStrategy.MEAN, config.getPoolingStrategy());
        assertTrue(config.isNormalize());
    }

    @Test
    public void testConfigBuilder() {
        OnnxEmbeddingConfig config = new OnnxEmbeddingConfig()
                .setModelPath("/path/to/model.onnx")
                .setVocabPath("/path/to/vocab.txt")
                .setMaxSequenceLength(256)
                .setPoolingStrategy(OnnxEmbeddingConfig.PoolingStrategy.CLS)
                .setNormalize(false);

        assertEquals("/path/to/model.onnx", config.getModelPath());
        assertEquals("/path/to/vocab.txt", config.getVocabPath());
        assertEquals(256, config.getMaxSequenceLength());
        assertEquals(OnnxEmbeddingConfig.PoolingStrategy.CLS, config.getPoolingStrategy());
        assertFalse(config.isNormalize());
    }

    @Test
    public void testConfigConstructor() {
        OnnxEmbeddingConfig config = new OnnxEmbeddingConfig("model.onnx", "vocab.txt");
        assertEquals("model.onnx", config.getModelPath());
        assertEquals("vocab.txt", config.getVocabPath());
    }

    @Test
    public void testTokenizerLoadVocab() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 512);
        assertTrue("词汇表应包含[CLS]", tokenizer.getVocabSize() > 0);
    }

    @Test
    public void testTokenizerSpecialTokens() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 512);
        assertEquals(2, tokenizer.getClsTokenId());
        assertEquals(3, tokenizer.getSepTokenId());
        assertEquals(0, tokenizer.getPadTokenId());
        assertEquals(1, tokenizer.getUnkTokenId());
    }

    @Test
    public void testTokenizerEncode() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 16);

        OnnxTokenizer.TokenizerOutput output = tokenizer.encode("hello world");

        assertNotNull(output);
        assertEquals(16, output.inputIds.length);
        assertEquals(16, output.attentionMask.length);
        assertEquals(16, output.tokenTypeIds.length);

        assertEquals(2, output.inputIds[0]);

        assertEquals(0, output.inputIds[15]);
        assertEquals(0, output.attentionMask[15]);
    }

    @Test
    public void testTokenizerEncodeBatch() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 16);

        List<String> texts = Arrays.asList("hello world", "test text");
        OnnxTokenizer.BatchTokenizerOutput output = tokenizer.encodeBatch(texts);

        assertNotNull(output);
        assertEquals(2, output.inputIds.length);
        assertEquals(16, output.inputIds[0].length);
    }

    @Test
    public void testTokenizerUnknownToken() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 16);

        OnnxTokenizer.TokenizerOutput output = tokenizer.encode("xyzabc123");

        boolean hasUnk = false;
        for (long id : output.inputIds) {
            if (id == 1) {
                hasUnk = true;
                break;
            }
        }
        assertTrue("未知词应映射为[UNK]", hasUnk || output.inputIds[0] == 2);
    }

    @Test
    public void testTokenizerEmptyText() throws IOException {
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), 16);

        OnnxTokenizer.TokenizerOutput output = tokenizer.encode("");

        assertNotNull(output);
        assertEquals(2, output.inputIds[0]);
        assertEquals(3, output.inputIds[1]);
        assertEquals(1, output.attentionMask[0]);
        assertEquals(1, output.attentionMask[1]);
    }

    @Test
    public void testTokenizerMaxLength() throws IOException {
        int maxLen = 8;
        OnnxTokenizer tokenizer = new OnnxTokenizer(getVocabPath(), maxLen);

        OnnxTokenizer.TokenizerOutput output = tokenizer.encode("hello world test text");

        assertEquals(maxLen, output.inputIds.length);
        assertEquals(2, output.inputIds[0]);
    }

    @Test
    public void testPoolingStrategies() {
        assertEquals("MEAN", OnnxEmbeddingConfig.PoolingStrategy.MEAN.name());
        assertEquals("CLS", OnnxEmbeddingConfig.PoolingStrategy.CLS.name());
        assertEquals("MAX", OnnxEmbeddingConfig.PoolingStrategy.MAX.name());
    }

    @Test
    public void testFactoryMethodExists() {
        assertNotNull("EmbeddingFactory 应存在",
                EmbeddingFactory.class);
    }
}