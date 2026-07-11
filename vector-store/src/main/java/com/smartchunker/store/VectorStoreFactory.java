package com.smartchunker.store;

import com.smartchunker.store.impl.HnswVectorStore;
import com.smartchunker.store.impl.InMemoryVectorStore;

public class VectorStoreFactory {

    public static VectorStore createInMemoryStore() {
        return new InMemoryVectorStore();
    }

    public static VectorStore createHnswStore() {
        return new HnswVectorStore();
    }

    public static VectorStore createHnswStore(int m, int efConstruction, int efSearch) {
        return new HnswVectorStore(m, efConstruction, efSearch);
    }
}