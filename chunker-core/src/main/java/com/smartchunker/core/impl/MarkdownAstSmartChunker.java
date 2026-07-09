package com.smartchunker.core.impl;

import com.smartchunker.core.SmartChunker;
import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MarkdownAstSmartChunker implements SmartChunker {

    private static final Parser PARSER = Parser.builder().build();

    @Override
    public List<DocumentChunk> process(File file, ChunkConfig config) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return process(content, config);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public List<DocumentChunk> process(String markdownContent, ChunkConfig config) {
        Document document = PARSER.parse(markdownContent);
        List<ChunkSegment> segments = extractSegments(document);
        return buildChunks(segments, config);
    }

    private List<ChunkSegment> extractSegments(Document document) {
        List<ChunkSegment> segments = new ArrayList<>();
        Deque<String> headingStack = new ArrayDeque<>();

        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(Heading.class, heading -> {
                    int level = heading.getLevel();
                    while (headingStack.size() >= level) {
                        headingStack.pollLast();
                    }
                    headingStack.addLast(heading.getText().toString());
                    segments.add(new ChunkSegment(
                            headingStack,
                            heading.getText().toString(),
                            heading.getLineNumber(),
                            SegmentType.HEADING
                    ));
                }),
                new VisitHandler<>(FencedCodeBlock.class, codeBlock -> {
                    segments.add(new ChunkSegment(
                            headingStack,
                            codeBlock.getChars().toString(),
                            codeBlock.getLineNumber(),
                            SegmentType.CODE_BLOCK
                    ));
                }),
                new VisitHandler<>(Paragraph.class, paragraph -> {
                    String text = paragraph.getChars().toString().trim();
                    if (!text.isEmpty()) {
                        segments.add(new ChunkSegment(
                                headingStack,
                                text,
                                paragraph.getLineNumber(),
                                SegmentType.PARAGRAPH
                        ));
                    }
                })
        );
        visitor.visit(document);
        return segments;
    }

    private List<DocumentChunk> buildChunks(List<ChunkSegment> segments, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (segments.isEmpty()) {
            return chunks;
        }

        StringBuilder buffer = new StringBuilder();
        String currentContext = "";
        int chunkStartLine = segments.get(0).lineNumber;
        int chunkEndLine = chunkStartLine;

        for (ChunkSegment seg : segments) {
            String ctx = seg.buildContextPath();
            if (!ctx.isEmpty()) {
                currentContext = ctx;
            }

            if (seg.type == SegmentType.HEADING) {
                if (buffer.length() > 0) {
                    chunks.add(new DocumentChunk(
                            buffer.toString().trim(), currentContext, chunkStartLine, chunkEndLine));
                    buffer.setLength(0);
                }
                chunkStartLine = seg.lineNumber;
                chunkEndLine = seg.lineNumber;
                buffer.append(seg.content).append("\n\n");
                continue;
            }

            boolean isAtomic = (seg.type == SegmentType.CODE_BLOCK);
            boolean wouldExceed = buffer.length() + seg.content.length() > config.getMaxChunkSize();
            boolean shouldSplit = wouldExceed && !isAtomic;

            if (shouldSplit && buffer.length() > 0) {
                chunks.add(new DocumentChunk(
                        buffer.toString().trim(), currentContext, chunkStartLine, chunkEndLine));
                buffer.setLength(0);
                chunkStartLine = seg.lineNumber;
            }

            buffer.append(seg.content).append("\n\n");
            chunkEndLine = seg.lineNumber;
        }

        if (buffer.length() > 0) {
            chunks.add(new DocumentChunk(
                    buffer.toString().trim(), currentContext, chunkStartLine, chunkEndLine));
        }

        return chunks;
    }

    private enum SegmentType {
        HEADING, PARAGRAPH, CODE_BLOCK
    }

    private static class ChunkSegment {
        final Deque<String> headingStack;
        final String content;
        final int lineNumber;
        final SegmentType type;

        ChunkSegment(Deque<String> headingStack, String content, int lineNumber, SegmentType type) {
            this.headingStack = new ArrayDeque<>(headingStack);
            this.content = content;
            this.lineNumber = lineNumber;
            this.type = type;
        }

        String buildContextPath() {
            if (headingStack.isEmpty()) {
                return "";
            }
            return String.join(" > ", headingStack);
        }
    }
}