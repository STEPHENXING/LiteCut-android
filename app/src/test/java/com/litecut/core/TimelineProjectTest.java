package com.litecut.core;

import com.litecut.core.timeline.MediaAsset;
import com.litecut.core.timeline.TimelineMapping;
import com.litecut.core.timeline.TimelineProject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class TimelineProjectTest {
    @Test
    public void appendAssetCreatesDefaultSelectedClip() {
        TimelineProject project = new TimelineProject();
        MediaAsset asset = project.appendAsset("content://video/1", "素材1", 10_000L);

        assertEquals(1, project.assets().size());
        assertEquals(asset.id, project.clips().get(0).assetId);
        assertEquals(10_000L, project.totalDurationMs());
        assertNotNull(project.selectedClip());
    }

    @Test
    public void splitSelectedClipAtPlayheadCreatesTwoAdjacentClips() {
        TimelineProject project = new TimelineProject();
        project.appendAsset("content://video/1", "素材1", 10_000L);
        project.setPlayheadMs(4_000L);

        project.splitSelectedAtPlayhead();

        assertEquals(2, project.clips().size());
        assertEquals(0L, project.clips().get(0).sourceInMs);
        assertEquals(4_000L, project.clips().get(0).sourceOutMs);
        assertEquals(4_000L, project.clips().get(1).sourceInMs);
        assertEquals(10_000L, project.clips().get(1).sourceOutMs);
        assertEquals(10_000L, project.totalDurationMs());
    }

    @Test
    public void duplicateSelectedClipInsertsCopyAfterSelection() {
        TimelineProject project = new TimelineProject();
        project.appendAsset("content://video/1", "素材1", 5_000L);

        project.duplicateSelectedClip();

        assertEquals(2, project.clips().size());
        assertEquals(10_000L, project.totalDurationMs());
        assertEquals(project.clips().get(0).assetId, project.clips().get(1).assetId);
    }

    @Test
    public void deleteSelectedClipKeepsSourceAsset() {
        TimelineProject project = new TimelineProject();
        project.appendAsset("content://video/1", "素材1", 5_000L);

        project.deleteSelectedClip();

        assertEquals(0, project.clips().size());
        assertEquals(1, project.assets().size());
        assertEquals(0L, project.totalDurationMs());
    }

    @Test
    public void moveClipReordersWithoutChangingDurations() {
        TimelineProject project = new TimelineProject();
        MediaAsset first = project.appendAsset("content://video/1", "素材1", 4_000L);
        MediaAsset second = project.appendAsset("content://video/2", "素材2", 6_000L);

        project.moveClip(0, 1);

        assertEquals(second.id, project.clips().get(0).assetId);
        assertEquals(first.id, project.clips().get(1).assetId);
        assertEquals(10_000L, project.totalDurationMs());
    }

    @Test
    public void timelineMappingHandlesSplitAndDuplicatedClips() {
        TimelineProject project = new TimelineProject();
        project.appendAsset("content://video/1", "素材1", 10_000L);
        project.setPlayheadMs(4_000L);
        project.splitSelectedAtPlayhead();
        project.duplicateSelectedClip();

        TimelineMapping secondClip = project.mapTimelineToSource(4_500L);
        TimelineMapping duplicateClip = project.mapTimelineToSource(12_500L);

        assertEquals(project.clips().get(1).id, secondClip.clipId);
        assertEquals(500L, secondClip.clipLocalOffsetMs);
        assertEquals(4_500L, secondClip.sourceTimeMs);
        assertEquals(project.clips().get(2).id, duplicateClip.clipId);
        assertEquals(6_500L, duplicateClip.sourceTimeMs);
    }

    @Test
    public void timelineMappingHandlesReorderedClips() {
        TimelineProject project = new TimelineProject();
        MediaAsset first = project.appendAsset("content://video/1", "素材1", 4_000L);
        MediaAsset second = project.appendAsset("content://video/2", "素材2", 6_000L);

        project.moveClip(0, 1);
        TimelineMapping mapping = project.mapTimelineToSource(1_000L);

        assertEquals(second.id, project.clips().get(0).assetId);
        assertEquals(project.clips().get(0).id, mapping.clipId);
        assertEquals(1_000L, mapping.sourceTimeMs);
        assertEquals(first.id, project.clips().get(1).assetId);
    }

    @Test
    public void mappingClampsOutsideTimelineAndClipBounds() {
        TimelineProject project = new TimelineProject();
        project.appendAsset("content://video/1", "素材1", 10_000L);
        project.setPlayheadMs(4_000L);
        project.splitSelectedAtPlayhead();

        TimelineMapping beforeStart = project.mapTimelineToSource(-1_000L);
        TimelineMapping afterEnd = project.mapTimelineToSource(50_000L);
        TimelineMapping sourceAfterClip = project.mapSourceToTimeline(project.clips().get(1).id, 50_000L);

        assertEquals(0L, beforeStart.timelineTimeMs);
        assertEquals(0L, beforeStart.sourceTimeMs);
        assertEquals(10_000L, afterEnd.timelineTimeMs);
        assertEquals(10_000L, afterEnd.sourceTimeMs);
        assertEquals(10_000L, sourceAfterClip.timelineTimeMs);
        assertEquals(10_000L, sourceAfterClip.sourceTimeMs);
    }
}
