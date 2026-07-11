package com.litecut.core.timeline;

public final class TimelineViewport {
    private TimelineViewport() {
    }

    public static float contentWidth(long durationMs, float pxPerSecond, float minWidth) {
        float seconds = Math.max(0L, durationMs) / 1000f;
        return Math.max(minWidth, seconds * Math.max(1f, pxPerSecond));
    }

    public static float clampScroll(float scrollX, float contentWidth, float viewportWidth) {
        float maxScroll = Math.max(0f, contentWidth - Math.max(0f, viewportWidth));
        return Math.max(0f, Math.min(maxScroll, scrollX));
    }

    public static float xForTime(long timelineMs, float pxPerSecond, float scrollX, float leftPadding) {
        return leftPadding + Math.max(0L, timelineMs) / 1000f * Math.max(1f, pxPerSecond) - Math.max(0f, scrollX);
    }

    public static long timeForX(float x, float pxPerSecond, float scrollX, float leftPadding, long totalDurationMs) {
        float contentX = Math.max(0f, x - leftPadding + Math.max(0f, scrollX));
        long timelineMs = (long) (contentX / Math.max(1f, pxPerSecond) * 1000f);
        return Math.max(0L, Math.min(Math.max(0L, totalDurationMs), timelineMs));
    }
}
