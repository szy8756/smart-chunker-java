package com.smartchunker.engine;

import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.core.model.DocumentChunk;
import com.smartchunker.engine.model.BatchResult;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ChunkEngineTest {

    private ChunkEngine engine = new ChunkEngine();

    @After
    public void tearDown() {
        engine.shutdown();
    }

    @Test
    public void testProcessDirectory() {
        ClassLoader classLoader = getClass().getClassLoader();
        File testDir = new File(classLoader.getResource("test-docs").getFile());

        ChunkConfig config = new ChunkConfig(500, 50);
        BatchResult result = engine.processDirectory(testDir, config);

        assertNotNull(result);
        assertEquals(2, result.getFileCount());
        assertTrue("总切片数应该大于0", result.getTotalChunkCount() > 0);

        System.out.println("处理了 " + result.getFileCount() + " 个文件");
        System.out.println("生成了 " + result.getTotalChunkCount() + " 个 Chunk");
        System.out.println("耗时: " + result.getElapsedMs() + " ms");

        Map<String, List<DocumentChunk>> chunks = result.getFileChunks();
        assertTrue("应该包含 doc1.md", chunks.containsKey("doc1.md"));
        assertTrue("应该包含 doc2.md", chunks.containsKey("doc2.md"));

        // 验证 doc1 中代码块被保留
        boolean hasCode = chunks.get("doc1.md").stream()
                .anyMatch(c -> c.getContent().contains("System.out.println"));
        assertTrue("doc1 应该包含代码块", hasCode);
    }
}