package com.litecut.core;

import com.litecut.core.model.AudioStreamInfo;
import com.litecut.core.model.Fraction;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.model.VideoStreamInfo;
import com.litecut.core.plan.FfmpegCommand;
import com.litecut.core.plan.OperationPlanner;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class OperationPlannerTest {
    @Test
    public void cutUsesStreamCopyAndTimestampNormalization() throws Exception {
        FfmpegCommand command = new OperationPlanner().planCut(
                metadata(true),
                new File("input.mp4"),
                new File("out.mp4"),
                500L,
                2500L
        );

        assertTrue(command.arguments.contains("-c"));
        assertTrue(command.arguments.contains("copy"));
        assertTrue(command.arguments.contains("-avoid_negative_ts"));
        assertTrue(command.arguments.contains("make_zero"));
        assertFalse(command.arguments.contains("-vf"));
    }

    @Test
    public void audioLessVideoDoesNotAddAudioMap() throws Exception {
        FfmpegCommand command = new OperationPlanner().planCut(
                metadata(false),
                new File("input.mp4"),
                new File("out.mp4"),
                500L,
                2500L
        );

        assertFalse(command.arguments.contains("0:a?"));
        assertTrue(command.arguments.contains("0:v:0"));
    }

    @Test
    public void extractAudioRequiresAudioStream() throws Exception {
        FfmpegCommand command = new OperationPlanner().planExtractAudio(
                metadata(true),
                new File("input.mp4"),
                new File("out.m4a")
        );

        assertTrue(command.arguments.contains("-vn"));
        assertTrue(command.arguments.contains("-c:a"));
        assertTrue(command.arguments.contains("copy"));
    }

    private static MediaMetadata metadata(boolean withAudio) {
        return new MediaMetadata(
                "input.mp4",
                10_000L,
                new VideoStreamInfo(0, "h264", 1920, 1080, new Fraction(30, 1)),
                withAudio ? Collections.singletonList(new AudioStreamInfo(1, "aac")) : Collections.emptyList(),
                Arrays.asList(0L, 2000L, 4000L, 6000L, 8000L)
        );
    }
}
