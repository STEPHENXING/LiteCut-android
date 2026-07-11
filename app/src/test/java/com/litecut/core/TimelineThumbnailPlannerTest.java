package com.litecut.core;

import com.litecut.core.thumbnail.TimelineThumbnailPlanner;
import com.litecut.core.thumbnail.TimelineThumbnailRequest;
import com.litecut.core.timeline.TimelineClip;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TimelineThumbnailPlannerTest {
    @Test
    public void thumbnailCountIsBoundedForLongClips() {
        TimelineThumbnailPlanner planner = new TimelineThumbnailPlanner();

        assertEquals(1, planner.thumbnailCount(5_000L));
        assertEquals(2, planner.thumbnailCount(20_000L));
        assertEquals(4, planner.thumbnailCount(90_000L));
        assertEquals(TimelineThumbnailPlanner.MAX_THUMBNAILS_PER_CLIP, planner.thumbnailCount(3_600_000L));
    }

    @Test
    public void requestsUseAssetAndSourceTimeStableCacheKeys() {
        TimelineThumbnailPlanner planner = new TimelineThumbnailPlanner();
        TimelineClip clip = new TimelineClip("clip_1", "asset_1", 10_000L, 70_000L);

        List<TimelineThumbnailRequest> requests = planner.planForClip(clip, "asset:1", new File("cache"));

        assertEquals(4, requests.size());
        assertEquals("asset:1", requests.get(0).assetId);
        assertEquals(10_000L, requests.get(0).sourceTimeMs);
        assertTrue(requests.get(0).outputFile.getName().startsWith("asset_1_10000"));
    }
}
