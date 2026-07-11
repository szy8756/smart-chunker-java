package com.smartchunker.store.impl;

public class HnswConfig {

    private final int m;
    private final int efConstruction;
    private final int efSearch;
    private final int mMax;
    private final int mMax0;

    public HnswConfig() {
        this(16, 200, 100);
    }

    public HnswConfig(int m, int efConstruction, int efSearch) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
        this.mMax = m;
        this.mMax0 = m * 2;
    }

    public int getM() {
        return m;
    }

    public int getEfConstruction() {
        return efConstruction;
    }

    public int getEfSearch() {
        return efSearch;
    }

    public int getMMax() {
        return mMax;
    }

    public int getMMax0() {
        return mMax0;
    }
}