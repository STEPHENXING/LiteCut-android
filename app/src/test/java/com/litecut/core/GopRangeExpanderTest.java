package com.litecut.core;

import com.litecut.core.model.EditRange;
import com.litecut.core.plan.GopRangeExpander;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public final class GopRangeExpanderTest {
    @Test
    public void expandsInToPreviousKeyframeAndOutToNextKeyframe() throws Exception {
        EditRange range = new GopRangeExpander().expand(
                2500L,
                5200L,
                10000L,
                Arrays.asList(0L, 2000L, 4000L, 6000L, 8000L)
        );

        assertEquals(2000L, range.effectiveInMs);
        assertEquals(6000L, range.effectiveOutMs);
    }

    @Test
    public void usesFileEndWhenOutIsInLastGop() throws Exception {
        EditRange range = new GopRangeExpander().expand(
                8100L,
                9200L,
                10000L,
                Arrays.asList(0L, 2000L, 4000L, 6000L, 8000L)
        );

        assertEquals(8000L, range.effectiveInMs);
        assertEquals(10000L, range.effectiveOutMs);
    }
}
