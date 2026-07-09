package com.smartchunker.core;

import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MarkdownAstSmartChunkerTest {

    @Test
    public void testBasicChunking() {
        String markdown = "# 第一章 概述\n"
                + "这是第一章的内容，介绍项目背景。\n\n"
                + "## 1.1 配置说明\n"
                + "配置文件位于 /etc/app.conf。\n\n"
                + "```java\n"
                + "public class App {\n"
                + "    public static void main(String[] args) {\n"
                + "        System.out.println(\"Hello\");\n"
                + "    }\n"
                + "}\n"
                + "```\n\n"
                + "配置完成后重启服务。\n";

        SmartChunker chunker = ChunkerFactory.createMarkdownChunker();
        ChunkConfig config = new ChunkConfig(500, 50);
        List<DocumentChunk> chunks = chunker.process(markdown, config);

        assertNotNull(chunks);
        assertTrue("应该至少有一个 Chunk", chunks.size() > 0);

        for (DocumentChunk chunk : chunks) {
            System.out.println("【上下文路径】: " + chunk.getContextPath());
            System.out.println("【内容】: \n" + chunk.getContent());
            System.out.println("【行号】: " + chunk.getStartLine() + " - " + chunk.getEndLine());
            System.out.println("--------------------------------------------------");
        }

        boolean hasCodeBlock = chunks.stream()
                .anyMatch(c -> c.getContent().contains("System.out.println"));
        assertTrue("代码块应被保留", hasCodeBlock);

        boolean hasContext = chunks.stream()
                .anyMatch(c -> c.getContextPath().contains("1.1 配置说明"));
        assertTrue("应包含上下文路径", hasContext);
    }
}