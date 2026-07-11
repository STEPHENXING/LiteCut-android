package com.litecut.android;

public final class FfmpegResult {
    public final boolean success;
    public final int returnCode;
    public final String output;
    public final String error;

    public FfmpegResult(boolean success, int returnCode, String output, String error) {
        this.success = success;
        this.returnCode = returnCode;
        this.output = output == null ? "" : output;
        this.error = error == null ? "" : error;
    }
}
