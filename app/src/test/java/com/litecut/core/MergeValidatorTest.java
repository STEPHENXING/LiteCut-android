package com.litecut.core;

import com.litecut.core.model.Fraction;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.model.VideoStreamInfo;
import com.litecut.core.plan.OperationPlanner;
import com.litecut.core.plan.PlanException;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public final class MergeValidatorTest {
    @Test
    public void compatibleFilesCreateConcatPlan() throws Exception {
        assertTrue(new OperationPlanner().planMerge(
                Arrays.asList(metadata("h264", 1920, 1080, 30), metadata("h264", 1920, 1080, 30)),
                Arrays.asList(new File("a.mp4"), new File("b.mp4")),
                new File("concat.txt"),
                new File("merged.mp4")
        ).arguments.contains("concat"));
    }

    @Test(expected = PlanException.class)
    public void mismatchedResolutionIsRejected() throws Exception {
        new OperationPlanner().planMerge(
                Arrays.asList(metadata("h264", 1920, 1080, 30), metadata("h264", 1280, 720, 30)),
                Arrays.asList(new File("a.mp4"), new File("b.mp4")),
                new File("concat.txt"),
                new File("merged.mp4")
        );
    }

    private static MediaMetadata metadata(String codec, int width, int height, int fps) {
        return new MediaMetadata(
                "input.mp4",
                10_000L,
                new VideoStreamInfo(0, codec, width, height, new Fraction(fps, 1)),
                Collections.emptyList(),
                Arrays.asList(0L, 2000L, 4000L)
        );
    }
}
