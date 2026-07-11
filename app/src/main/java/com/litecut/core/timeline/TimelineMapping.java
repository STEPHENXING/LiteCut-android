package com.litecut.core.timeline;

public final class TimelineMapping {
    public final String clipId;
    public final long clipLocalOffsetMs;
    public final long sourceTimeMs;
    public final long timelineTimeMs;

    public TimelineMapping(String clipId, long clipLocalOffsetMs, long sourceTimeMs, long timelineTimeMs) {
        this.clipId = clipId;
        this.clipLocalOffsetMs = Math.max(0L, clipLocalOffsetMs);
        this.sourceTimeMs = Math.max(0L, sourceTimeMs);
        this.timelineTimeMs = Math.max(0L, timelineTimeMs);
    }
}
