package com.litecut.core.progress;

import java.io.File;

public final class SizeProgressEstimator {
    public int percent(File outputFile, long expectedBytes) {
        if (outputFile == null || expectedBytes <= 0L) {
            return 0;
        }
        long currentBytes = outputFile.exists() ? outputFile.length() : 0L;
        return (int) Math.max(0L, Math.min(99L, (currentBytes * 100L) / expectedBytes));
    }
}
