package com.litecut.core.model;

public final class VideoStreamInfo {
    public final int index;
    public final String codecName;
    public final int width;
    public final int height;
    public final Fraction frameRate;

    public VideoStreamInfo(int index, String codecName, int width, int height, Fraction frameRate) {
        this.index = index;
        this.codecName = codecName == null ? "" : codecName;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate == null ? new Fraction(0, 1) : frameRate;
    }
}
