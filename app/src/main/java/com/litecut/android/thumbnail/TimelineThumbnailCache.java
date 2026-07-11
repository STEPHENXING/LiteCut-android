package com.litecut.android.thumbnail;

import android.content.Context;

import java.io.File;

public final class TimelineThumbnailCache {
    private final File cacheDir;

    public TimelineThumbnailCache(Context context) {
        this.cacheDir = new File(context.getCacheDir(), "timeline-thumbnails");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public File directory() {
        return cacheDir;
    }
}
