package com.smartchunker.core;

import com.smartchunker.core.impl.MarkdownAstSmartChunker;

public class ChunkerFactory {
    public static SmartChunker createMarkdownChunker() {
        return new MarkdownAstSmartChunker();
    }
}
