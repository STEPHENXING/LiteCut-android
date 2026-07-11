package com.litecut.core.probe;

import com.litecut.core.plan.FfmpegCommand;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public final class FfprobePlan {
    public FfmpegCommand metadata(File input) {
        return new FfmpegCommand(
                FfmpegCommand.Kind.FFPROBE,
                Arrays.asList(
                        "-v", "error",
                        "-show_entries", "format=duration:stream=index,codec_type,codec_name,width,height,r_frame_rate",
                        "-of", "json",
                        input.getAbsolutePath()
                ),
                null,
                Collections.emptyList()
        );
    }

    public FfmpegCommand keyframes(File input) {
        return new FfmpegCommand(
                FfmpegCommand.Kind.FFPROBE,
                Arrays.asList(
                        "-v", "error",
                        "-select_streams", "v:0",
                        "-show_frames",
                        "-show_entries", "frame=key_frame,best_effort_timestamp_time,pkt_pts_time",
                        "-of", "json",
                        input.getAbsolutePath()
                ),
                null,
                Collections.emptyList()
        );
    }
}
