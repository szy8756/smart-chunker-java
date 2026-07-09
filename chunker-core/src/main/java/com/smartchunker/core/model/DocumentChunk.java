package com.smartchunker.core.model;

public class DocumentChunk {

    private final String content; // 切片的正文内容容
    private final String contextPath; // 上级标题路径
    private final int startLine; // 开始行号
    private final int endLine; // 结束行号

    public DocumentChunk(String content, String contextPath, int startLine, int endLine) {
        this.content = content;
        this.contextPath = contextPath;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getContent() {
        return content;
    }

    public String getContextPath() {
        return contextPath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int length() {
        return content == null ? 0 : content.length();
    }
}
