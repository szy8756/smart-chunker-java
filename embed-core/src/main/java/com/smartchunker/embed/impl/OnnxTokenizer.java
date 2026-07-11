package com.smartchunker.embed.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnnxTokenizer {

    private final Map<String, Integer> vocab;
    private final Map<Integer, String> idToToken;
    private final int maxLength;
    private final int clsTokenId;
    private final int sepTokenId;
    private final int padTokenId;
    private final int unkTokenId;
    private final int maskTokenId;

    private static final String CLS = "[CLS]";
    private static final String SEP = "[SEP]";
    private static final String PAD = "[PAD]";
    private static final String UNK = "[UNK]";
    private static final String MASK = "[MASK]";

    public OnnxTokenizer(String vocabPath, int maxLength) throws IOException {
        this.maxLength = maxLength;
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();

        Path path = Paths.get(vocabPath);
        List<String> lines;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            String token = lines.get(i);
            vocab.put(token, i);
            idToToken.put(i, token);
        }

        this.clsTokenId = getTokenId(CLS);
        this.sepTokenId = getTokenId(SEP);
        this.padTokenId = getTokenId(PAD);
        this.unkTokenId = getTokenId(UNK);
        this.maskTokenId = getTokenId(MASK);
    }

    public int getTokenId(String token) {
        return vocab.getOrDefault(token, unkTokenId);
    }

    public String getToken(int id) {
        return idToToken.getOrDefault(id, UNK);
    }

    public int getVocabSize() {
        return vocab.size();
    }

    public int getClsTokenId() {
        return clsTokenId;
    }

    public int getSepTokenId() {
        return sepTokenId;
    }

    public int getPadTokenId() {
        return padTokenId;
    }

    public int getUnkTokenId() {
        return unkTokenId;
    }

    public int getMaskTokenId() {
        return maskTokenId;
    }

    public TokenizerOutput encode(String text) {
        List<Integer> tokenIds = new ArrayList<>();
        tokenIds.add(clsTokenId);

        List<String> basicTokens = basicTokenize(text);
        for (String token : basicTokens) {
            List<Integer> subTokens = wordPiece(token);
            for (int subId : subTokens) {
                if (tokenIds.size() < maxLength - 1) {
                    tokenIds.add(subId);
                }
            }
        }

        if (tokenIds.size() < maxLength) {
            tokenIds.add(sepTokenId);
        }

        int seqLen = Math.min(tokenIds.size(), maxLength);
        long[] inputIds = new long[maxLength];
        long[] attentionMask = new long[maxLength];
        long[] tokenTypeIds = new long[maxLength];

        for (int i = 0; i < seqLen; i++) {
            inputIds[i] = tokenIds.get(i);
            attentionMask[i] = 1;
            tokenTypeIds[i] = 0;
        }
        for (int i = seqLen; i < maxLength; i++) {
            inputIds[i] = padTokenId;
            attentionMask[i] = 0;
            tokenTypeIds[i] = 0;
        }

        return new TokenizerOutput(inputIds, attentionMask, tokenTypeIds, seqLen);
    }

    public BatchTokenizerOutput encodeBatch(List<String> texts) {
        int batchSize = texts.size();
        long[][] inputIds = new long[batchSize][maxLength];
        long[][] attentionMask = new long[batchSize][maxLength];
        long[][] tokenTypeIds = new long[batchSize][maxLength];

        for (int i = 0; i < batchSize; i++) {
            TokenizerOutput output = encode(texts.get(i));
            inputIds[i] = output.inputIds;
            attentionMask[i] = output.attentionMask;
            tokenTypeIds[i] = output.tokenTypeIds;
        }

        return new BatchTokenizerOutput(inputIds, attentionMask, tokenTypeIds);
    }

    private List<String> basicTokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        text = text.toLowerCase().trim();

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (isChineseChar(ch)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            } else if (Character.isWhitespace(ch)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else if (isPunctuation(ch)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private List<Integer> wordPiece(String token) {
        List<Integer> result = new ArrayList<>();

        if (vocab.containsKey(token)) {
            result.add(vocab.get(token));
            return result;
        }

        boolean isBad = false;
        int start = 0;
        List<String> subTokens = new ArrayList<>();

        while (start < token.length()) {
            int end = token.length();
            String curSubstr = null;

            while (start < end) {
                String substr = (start > 0 ? "##" : "") + token.substring(start, end);
                if (vocab.containsKey(substr)) {
                    curSubstr = substr;
                    break;
                }
                end--;
            }

            if (curSubstr == null) {
                isBad = true;
                break;
            }

            subTokens.add(curSubstr);
            start = end;
        }

        if (isBad) {
            result.add(unkTokenId);
        } else {
            for (String sub : subTokens) {
                result.add(vocab.get(sub));
            }
        }

        return result;
    }

    private boolean isChineseChar(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    private boolean isPunctuation(char ch) {
        return (ch >= 33 && ch <= 47) || (ch >= 58 && ch <= 64)
                || (ch >= 91 && ch <= 96) || (ch >= 123 && ch <= 126);
    }

    public static class TokenizerOutput {
        public final long[] inputIds;
        public final long[] attentionMask;
        public final long[] tokenTypeIds;
        public final int actualLength;

        public TokenizerOutput(long[] inputIds, long[] attentionMask, long[] tokenTypeIds, int actualLength) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
            this.actualLength = actualLength;
        }
    }

    public static class BatchTokenizerOutput {
        public final long[][] inputIds;
        public final long[][] attentionMask;
        public final long[][] tokenTypeIds;

        public BatchTokenizerOutput(long[][] inputIds, long[][] attentionMask, long[][] tokenTypeIds) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
        }
    }
}