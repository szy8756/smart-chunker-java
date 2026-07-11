package com.smartchunker.embed.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.smartchunker.embed.EmbeddingModel;
import com.smartchunker.embed.model.EmbeddingResult;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OnnxEmbeddingModel implements EmbeddingModel {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final OnnxTokenizer tokenizer;
    private final OnnxEmbeddingConfig config;
    private final int dimension;

    private static final String INPUT_IDS = "input_ids";
    private static final String ATTENTION_MASK = "attention_mask";
    private static final String TOKEN_TYPE_IDS = "token_type_ids";

    public OnnxEmbeddingModel(OnnxEmbeddingConfig config) throws OrtException, IOException {
        this.config = config;
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        this.session = env.createSession(config.getModelPath(), options);
        this.tokenizer = new OnnxTokenizer(config.getVocabPath(), config.getMaxSequenceLength());

        this.dimension = detectDimension();
    }

    private int detectDimension() throws OrtException {
        OrtSession.Result testResult = null;
        try {
            OnnxTokenizer.BatchTokenizerOutput tokens = tokenizer.encodeBatch(
                    Collections.singletonList("test"));
            long[][] ids = reshapeForBatch(tokens.inputIds, 1);
            long[][] mask = reshapeForBatch(tokens.attentionMask, 1);
            long[][] typeIds = reshapeForBatch(tokens.tokenTypeIds, 1);

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, ids);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, mask);
            OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, typeIds);

            Map<String, OnnxTensor> inputs = Map.of(
                    INPUT_IDS, inputIdsTensor,
                    ATTENTION_MASK, attentionMaskTensor,
                    TOKEN_TYPE_IDS, tokenTypeIdsTensor
            );

            testResult = session.run(inputs);
            float[][][] output = (float[][][]) testResult.get(0).getValue();
            return output[0][0].length;
        } finally {
            if (testResult != null) {
                testResult.close();
            }
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public EmbeddingResult embed(String text) {
        List<EmbeddingResult> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmbeddingResult> results = new ArrayList<>();

        int batchSize = texts.size();
        int maxLen = config.getMaxSequenceLength();

        OnnxTokenizer.BatchTokenizerOutput tokens = tokenizer.encodeBatch(texts);

        long[][] ids = reshapeForBatch(tokens.inputIds, batchSize);
        long[][] mask = reshapeForBatch(tokens.attentionMask, batchSize);
        long[][] typeIds = reshapeForBatch(tokens.tokenTypeIds, batchSize);

        OrtSession.Result ortResult = null;
        try {
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, ids);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, mask);
            OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, typeIds);

            Map<String, OnnxTensor> inputs = Map.of(
                    INPUT_IDS, inputIdsTensor,
                    ATTENTION_MASK, attentionMaskTensor,
                    TOKEN_TYPE_IDS, tokenTypeIdsTensor
            );

            ortResult = session.run(inputs);
            float[][][] hiddenStates = (float[][][]) ortResult.get(0).getValue();

            for (int i = 0; i < batchSize; i++) {
                float[] embedding = poolEmbedding(hiddenStates[i], tokens.attentionMask[i], maxLen);
                if (config.isNormalize()) {
                    embedding = normalize(embedding);
                }
                results.add(new EmbeddingResult(texts.get(i), embedding));
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX 推理失败: " + e.getMessage(), e);
        } finally {
            if (ortResult != null) {
                try {
                    ortResult.close();
                } catch (OrtException ignored) {
                }
            }
        }

        return results;
    }

    private float[] poolEmbedding(float[][] hiddenStates, long[] attentionMask, int seqLen) {
        float[] embedding = new float[dimension];
        OnnxEmbeddingConfig.PoolingStrategy strategy = config.getPoolingStrategy();

        switch (strategy) {
            case CLS:
                System.arraycopy(hiddenStates[0], 0, embedding, 0, dimension);
                break;

            case MAX:
                Arrays.fill(embedding, -Float.MAX_VALUE);
                for (int i = 0; i < seqLen; i++) {
                    if (attentionMask[i] == 1) {
                        for (int j = 0; j < dimension; j++) {
                            embedding[j] = Math.max(embedding[j], hiddenStates[i][j]);
                        }
                    }
                }
                break;

            case MEAN:
            default:
                int validTokens = 0;
                for (int i = 0; i < seqLen; i++) {
                    if (attentionMask[i] == 1) {
                        for (int j = 0; j < dimension; j++) {
                            embedding[j] += hiddenStates[i][j];
                        }
                        validTokens++;
                    }
                }
                if (validTokens > 0) {
                    for (int j = 0; j < dimension; j++) {
                        embedding[j] /= validTokens;
                    }
                }
                break;
        }

        return embedding;
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

    private long[][] reshapeForBatch(long[] flat, int batchSize) {
        int seqLen = flat.length / batchSize;
        long[][] result = new long[batchSize][seqLen];
        for (int i = 0; i < batchSize; i++) {
            System.arraycopy(flat, i * seqLen, result[i], 0, seqLen);
        }
        return result;
    }

    public void close() {
        try {
            session.close();
        } catch (OrtException ignored) {
        }
        try {
            env.close();
        } catch (OrtException ignored) {
        }
    }
}