package com.smartchunker.engine;

import com.smartchunker.core.config.ChunkConfig;
import com.smartchunker.engine.model.IndexResult;
import com.smartchunker.engine.model.SearchResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class VectorPipelineTest {

    private VectorPipeline pipeline;

    @Before
    public void setUp() {
        pipeline = new VectorPipeline();
    }

    @After
    public void tearDown() {
        pipeline.shutdown();
    }

    @Test
    public void testBuildIndex() {
        ClassLoader classLoader = getClass().getClassLoader();
        File testDir = new File(classLoader.getResource("test-docs").getFile());

        ChunkConfig config = new ChunkConfig(500, 50);
        IndexResult result = pipeline.buildIndex(testDir, config);

        assertNotNull(result);
        assertEquals(2, result.getFileCount());
        assertTrue("切片数应大于0", result.getChunkCount() > 0);
        assertEquals("向量数应等于切片数", result.getChunkCount(), result.getVectorCount());
        assertTrue("耗时应大于0", result.getElapsedMs() > 0);

        System.out.println("索引构建完成: " + result);
    }

    @Test
    public void testSearch() {
        ClassLoader classLoader = getClass().getClassLoader();
        File testDir = new File(classLoader.getResource("test-docs").getFile());

        ChunkConfig config = new ChunkConfig(500, 50);
        pipeline.buildIndex(testDir, config);

        List<SearchResult> results = pipeline.search("public class", 3);

        assertNotNull(results);
        assertTrue("应至少有一个结果", results.size() > 0);

        for (SearchResult r : results) {
            System.out.println("SearchResult: " + r);
            assertNotNull(r.getId());
            assertNotNull(r.getContent());
            assertTrue("得分应在0-1之间", r.getScore() >= -1.0f && r.getScore() <= 1.0f);
        }
    }

    @Test
    public void testSearchEmptyIndex() {
        List<SearchResult> results = pipeline.search("anything", 5);
        assertNotNull(results);
        assertTrue("空索引应返回空结果", results.isEmpty());
    }

    @Test
    public void testGetVectorCount() {
        assertEquals(0, pipeline.getVectorCount());

        ClassLoader classLoader = getClass().getClassLoader();
        File testDir = new File(classLoader.getResource("test-docs").getFile());

        ChunkConfig config = new ChunkConfig(500, 50);
        pipeline.buildIndex(testDir, config);

        assertTrue("向量数应大于0", pipeline.getVectorCount() > 0);
    }
}