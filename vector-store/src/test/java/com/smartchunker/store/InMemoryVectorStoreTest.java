package com.smartchunker.store;

import com.smartchunker.store.model.SearchHit;
import com.smartchunker.store.model.VectorDoc;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class InMemoryVectorStoreTest {

    private VectorStore store;

    @Before
    public void setUp() {
        store = VectorStoreFactory.createInMemoryStore();
    }

    @Test
    public void testEmptyStore() {
        assertEquals(0, store.size());
        assertTrue(store.getAll().isEmpty());
        assertTrue(store.search(new float[]{1, 0, 0}, 5).isEmpty());
    }

    @Test
    public void testAddAndSize() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        assertEquals(1, store.size());

        store.add("doc2", new float[]{0, 1, 0}, null);
        assertEquals(2, store.size());
    }

    @Test
    public void testSearchBasic() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        store.add("doc2", new float[]{0, 1, 0}, null);
        store.add("doc3", new float[]{0, 0, 1}, null);

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 2);

        assertEquals(2, results.size());
        assertEquals("doc1", results.get(0).getId());
        assertEquals(1.0f, results.get(0).getScore(), 0.0001f);
    }

    @Test
    public void testSearchTopK() {
        for (int i = 0; i < 10; i++) {
            float[] vec = new float[3];
            vec[0] = i * 0.1f;
            store.add("doc" + i, vec, null);
        }

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 3);
        assertEquals(3, results.size());
    }

    @Test
    public void testSearchWithMetadata() {
        Map<String, String> meta = new HashMap<>();
        meta.put("title", "Hello World");
        meta.put("author", "Alice");

        store.add("doc1", new float[]{1, 0, 0}, meta);

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 1);
        assertEquals(1, results.size());
        assertEquals("Hello World", results.get(0).getMetadata().get("title"));
        assertEquals("Alice", results.get(0).getMetadata().get("author"));
    }

    @Test
    public void testDelete() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        store.add("doc2", new float[]{0, 1, 0}, null);
        assertEquals(2, store.size());

        store.delete("doc1");
        assertEquals(1, store.size());

        List<SearchHit> results = store.search(new float[]{1, 0, 0}, 5);
        assertEquals(1, results.size());
        assertEquals("doc2", results.get(0).getId());
    }

    @Test
    public void testClear() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        store.add("doc2", new float[]{0, 1, 0}, null);
        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.getAll().isEmpty());
    }

    @Test
    public void testGetAll() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        store.add("doc2", new float[]{0, 1, 0}, null);

        List<VectorDoc> all = store.getAll();
        assertEquals(2, all.size());
    }

    @Test
    public void testDimensionMismatch() {
        store.add("doc1", new float[]{1, 0, 0}, null);

        try {
            store.search(new float[]{1, 0}, 1);
            fail("应抛出维度不匹配异常");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("维度不匹配"));
        }
    }

    @Test
    public void testSearchScoreOrder() {
        float[] query = new float[]{1, 0, 0};

        store.add("exact", new float[]{1, 0, 0}, null);
        store.add("near", new float[]{0.8f, 0.2f, 0}, null);
        store.add("far", new float[]{0, 1, 0}, null);

        List<SearchHit> results = store.search(query, 3);

        assertEquals(3, results.size());
        assertTrue("第一个结果应得分最高",
                results.get(0).getScore() >= results.get(1).getScore());
        assertTrue("第二个结果得分应不低于第三个",
                results.get(1).getScore() >= results.get(2).getScore());
    }

    @Test
    public void testDuplicateId() {
        store.add("doc1", new float[]{1, 0, 0}, null);
        store.add("doc1", new float[]{0, 1, 0}, null);

        assertEquals(1, store.size());
        assertEquals(0, store.getAll().get(0).getVector()[0], 0.0001f);
    }
}