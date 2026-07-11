package com.litecut.core.thumbnail;

import java.io.File;

public final class TimelineThumbnailRequest {
    public final String assetId;
    public final long sourceTimeMs;
    public final File outputFile;
    public final Status status;

    public TimelineThumbnailRequest(String assetId, long sourceTimeMs, File outputFile, Status status) {
        this.assetId = requireValue(assetId, "assetId");
        this.sourceTimeMs = Math.max(0L, sourceTimeMs);
        this.outputFile = outputFile;
        this.status = status == null ? Status.PENDING : status;
    }

    public TimelineThumbnailRequest withStatus(Status nextStatus) {
        return new TimelineThumbnailRequest(assetId, sourceTimeMs, outputFile, nextStatus);
    }

    public static String cacheKey(String assetId, long sourceTimeMs) {
        String safeAsset = requireValue(assetId, "assetId").replaceAll("[^A-Za-z0-9_.-]", "_");
        return safeAsset + "_" + Math.max(0L, sourceTimeMs) + ".jpg";
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }

    public enum Status {
        PENDING,
        READY,
        FAILED
    }
}
