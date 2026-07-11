package com.litecut.core.timeline;

public final class TimelineClip {
    public final String id;
    public final String assetId;
    public final long sourceInMs;
    public final long sourceOutMs;

    public TimelineClip(String id, String assetId, long sourceInMs, long sourceOutMs) {
        this.id = requireValue(id, "id");
        this.assetId = requireValue(assetId, "assetId");
        if (sourceOutMs < sourceInMs) {
            throw new IllegalArgumentException("sourceOutMs must be after sourceInMs.");
        }
        this.sourceInMs = Math.max(0L, sourceInMs);
        this.sourceOutMs = Math.max(0L, sourceOutMs);
    }

    public long durationMs() {
        return Math.max(0L, sourceOutMs - sourceInMs);
    }

    public TimelineClip withSourceRange(String newId, long newInMs, long newOutMs) {
        return new TimelineClip(newId, assetId, newInMs, newOutMs);
    }

    public TimelineClip copyAs(String newId) {
        return new TimelineClip(newId, assetId, sourceInMs, sourceOutMs);
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
