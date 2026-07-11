package com.litecut.core;

import com.litecut.core.timeline.TimelineViewport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TimelineViewportTest {
    @Test
    public void scrollIsClampedToContentBounds() {
        assertEquals(0f, TimelineViewport.clampScroll(-20f, 1000f, 300f), 0.01f);
        assertEquals(700f, TimelineViewport.clampScroll(900f, 1000f, 300f), 0.01f);
        assertEquals(0f, TimelineViewport.clampScroll(50f, 200f, 300f), 0.01f);
    }

    @Test
    public void timelineTimeAndXRoundTripThroughZoomAndScroll() {
        float x = TimelineViewport.xForTime(10_000L, 8f, 20f, 12f);
        long time = TimelineViewport.timeForX(x, 8f, 20f, 12f, 60_000L);

        assertEquals(72f, x, 0.01f);
        assertEquals(10_000L, time);
    }
}
