package com.litecut.core.model;

public final class AudioStreamInfo {
    public final int index;
    public final String codecName;

    public AudioStreamInfo(int index, String codecName) {
        this.index = index;
        this.codecName = codecName == null ? "" : codecName;
    }
}
