package com.smartchunker.store;

import com.smartchunker.store.model.SearchHit;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class HnswVectorStoreTest {

    private VectorStore store;

    @Before
    public void setUp() {
        store = VectorStoreFactory.createHnswStore();
    }

    @Test
    public void testEmptyStore() {
        assertEquals(0, store.size());
        assertTrue(store.search(new float[]{1, 0, 0}, 3).isEmpty());
    }

    @Test
    public void testAddAndSearch() {
        store.add("a", new float[]{1, 0, 0}, null);
        store.add("b", new float[]{0, 1, 0}, null);
        store.add("c", new float[]{0, 0, 1}, null);

        assertEquals(3, store.size());

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 2);
        assertEquals(2, results.size());
        assertTrue("第一个结果得分应大于0.9", results.get(0).getScore() > 0.9f);
    }

    @Test
    public void testSearchScoreOrder() {
        float[] q = new float[]{1, 0, 0};
        store.add("exact", new float[]{1, 0, 0}, null);
        store.add("near", new float[]{0.8f, 0.2f, 0}, null);
        store.add("far", new float[]{0, 1, 0}, null);

        List<SearchHit> results = store.search(q, 3);
        assertEquals(3, results.size());
        assertEquals("exact", results.get(0).getId());
        assertTrue("第一个得分应不低于第二个",
                results.get(0).getScore() >= results.get(1).getScore());
    }

    @Test
    public void testBulkInsert() {
        Random rand = new Random(42);
        int n = 200;

        for (int i = 0; i < n; i++) {
            float[] vec = new float[128];
            for (int j = 0; j < 128; j++) {
                vec[j] = rand.nextFloat() * 2 - 1;
            }
            store.add("doc" + i, vec, null);
        }

        assertEquals(n, store.size());
        assertTrue("HNSW 应返回结果", store.search(new float[128], 10).size() > 0);
    }

    @Test
    public void testDelete() {
        store.add("a", new float[]{1, 0, 0}, null);
        store.add("b", new float[]{0, 1, 0}, null);
        assertEquals(2, store.size());

        store.delete("a");
        assertEquals(1, store.size());

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 5);
        assertTrue("删除后剩余结果应不少于1", results.size() >= 1);
    }

    @Test
    public void testClear() {
        for (int i = 0; i < 50; i++) {
            store.add("doc" + i, new float[]{(float) i, 0, 0}, null);
        }
        assertEquals(50, store.size());

        store.clear();
        assertEquals(0, store.size());
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    public void testWithMetadata() {
        Map<String, String> meta = new HashMap<>();
        meta.put("title", "Test");
        store.add("doc1", new float[]{1, 0, 0}, meta);

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 1);
        assertEquals("Test", results.get(0).getMetadata().get("title"));
    }
}