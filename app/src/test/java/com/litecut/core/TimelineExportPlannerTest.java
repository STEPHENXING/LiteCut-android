package com.litecut.core;

import com.litecut.core.model.Fraction;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.model.VideoStreamInfo;
import com.litecut.core.plan.FfmpegCommand;
import com.litecut.core.plan.PlanException;
import com.litecut.core.plan.TimelineExportPlanner;
import com.litecut.core.timeline.MediaAsset;
import com.litecut.core.timeline.TimelineProject;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TimelineExportPlannerTest {
    @Test
    public void timelineExportUsesStreamCopyAndConcat() throws Exception {
        TimelineProject project = new TimelineProject();
        MediaAsset first = readyAsset(project, "a.mp4");
        MediaAsset second = readyAsset(project, "b.mp4");
        first.localWorkspaceFile.createNewFile();
        second.localWorkspaceFile.createNewFile();

        List<FfmpegCommand> commands = new TimelineExportPlanner().planTimelineExport(
                project,
                tempDir(),
                new File(tempDir(), "out.mp4")
        );

        String joined = commands.toString();
        boolean hasConcat = false;
        for (FfmpegCommand command : commands) {
            hasConcat = hasConcat || command.arguments.contains("concat");
            assertTrue(command.arguments.contains("copy"));
            assertTrue(command.arguments.contains("-avoid_negative_ts"));
            assertFalse(command.arguments.contains("-vf"));
        }
        assertTrue(hasConcat);
    }

    @Test(expected = PlanException.class)
    public void timelineExportRequiresLocalWorkspaceFile() throws Exception {
        TimelineProject project = new TimelineProject();
        readyAsset(project, "missing.mp4").localWorkspaceFile = new File("missing.mp4");

        new TimelineExportPlanner().planTimelineExport(project, tempDir(), new File(tempDir(), "out.mp4"));
    }

    private static MediaAsset readyAsset(TimelineProject project, String name) {
        MediaAsset asset = project.appendAsset("content://" + name, name, 10_000L);
        asset.localWorkspaceFile = new File(tempDir(), name);
        asset.markMetadataReady(new MediaMetadata(
                name,
                10_000L,
                new VideoStreamInfo(0, "h264", 1920, 1080, new Fraction(30, 1)),
                Collections.emptyList(),
                Arrays.asList(0L, 2_000L, 4_000L, 6_000L, 8_000L)
        ));
        return asset;
    }

    private static File tempDir() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "litecut-timeline-tests");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }
}
