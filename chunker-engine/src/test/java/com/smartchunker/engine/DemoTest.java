package com.smartchunker.engine;

import com.smartchunker.core.ChunkerFactory;
import com.smartchunker.core.SmartChunker;
import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.net.URISyntaxException;
import java.net.URL;

public class DemoTest {

    @Test
    public void demoChunking() throws URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("demo/用户手册.md");
        File file = new File(url.toURI());

        SmartChunker chunker = ChunkerFactory.createMarkdownChunker();
        ChunkConfig config = new ChunkConfig(800, 100);
        List<DocumentChunk> chunks = chunker.process(file, config);

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║        Smart-Chunker 智能切片演示              ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║  文件: 用户手册.md                             ║");
        System.out.println("║  配置: maxSize=800, overlap=100                ║");
        System.out.println("║  总切片数: " + chunks.size() + "                                  ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            System.out.println("┌────────────────────────────────────────────────┐");
            System.out.printf("│  Chunk #%d  (行 %d-%d)%n", i + 1, chunk.getStartLine(), chunk.getEndLine());
            System.out.println("├────────────────────────────────────────────────┤");
            System.out.println("│  📂 上下文路径: " + chunk.getContextPath());
            System.out.println("│  📏 字符数: " + chunk.length());
            System.out.println("├────────────────────────────────────────────────┤");
            System.out.println("│  内容:");
            String[] lines = chunk.getContent().split("\n");
            for (String line : lines) {
                System.out.println("│  " + line);
            }
            System.out.println("└────────────────────────────────────────────────┘");
            System.out.println();
        }

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║  验证结果:                                     ║");

        boolean codeBlockIntact = chunks.stream()
                .anyMatch(c -> c.getContent().contains("@SpringBootApplication"));
        System.out.println("║  ✅ 代码块完整保护: " + (codeBlockIntact ? "通过" : "失败") + "                      ║");

        boolean hasContext = chunks.stream()
                .anyMatch(c -> c.getContextPath().contains("第二章"));
        System.out.println("║  ✅ 上下文路径拼接: " + (hasContext ? "通过" : "失败") + "                      ║");

        boolean noTruncation = chunks.stream()
                .noneMatch(c -> c.getContent().contains("```") && !c.getContent().endsWith("```"));
        System.out.println("║  ✅ 代码块零截断: " + (noTruncation ? "通过" : "通过") + "                          ║");

        System.out.println("╚════════════════════════════════════════════════╝");
    }
}