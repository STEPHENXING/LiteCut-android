package com.litecut.android;

import com.litecut.core.model.MediaMetadata;
import com.litecut.core.probe.FfprobeJsonParser;
import com.litecut.core.probe.FfprobePlan;

import org.json.JSONException;

import java.io.File;

public final class MediaProbeRepository {
    private final FfmpegExecutor executor;
    private final FfprobePlan probePlan = new FfprobePlan();
    private final FfprobeJsonParser parser = new FfprobeJsonParser();

    public MediaProbeRepository(FfmpegExecutor executor) {
        this.executor = executor;
    }

    public MediaMetadata probe(File input) throws ProbeException {
        MediaMetadata metadata = probeBasic(input);
        return probeKeyframes(input, metadata);
    }

    public MediaMetadata probeBasic(File input) throws ProbeException {
        FfmpegResult metadata = executor.execute(probePlan.metadata(input));
        if (!metadata.success) {
            throw new ProbeException("无法读取媒体信息：" + readable(metadata));
        }
        try {
            MediaMetadata parsed = parser.parseMetadata(input.getAbsolutePath(), metadata.output);
            if (!parsed.hasVideo()) {
                throw new ProbeException("没有找到可读取的视频流。");
            }
            return parsed;
        } catch (JSONException | NullPointerException | NumberFormatException exception) {
            throw new ProbeException("ffprobe 返回了无法解析的信息：" + exception.getMessage());
        }
    }

    public MediaMetadata probeKeyframes(File input, MediaMetadata basicMetadata) throws ProbeException {
        FfmpegResult keyframes = executor.execute(probePlan.keyframes(input));
        if (!keyframes.success) {
            throw new ProbeException("无法读取关键帧：" + readable(keyframes));
        }

        try {
            return new MediaMetadata(
                    basicMetadata.sourcePath,
                    basicMetadata.durationMs,
                    basicMetadata.videoStream,
                    basicMetadata.audioStreams,
                    parser.parseKeyframes(keyframes.output)
            );
        } catch (JSONException | NullPointerException | NumberFormatException exception) {
            throw new ProbeException("关键帧信息无法解析：" + exception.getMessage());
        }
    }

    private static String readable(FfmpegResult result) {
        if (!result.error.isEmpty()) {
            return result.error;
        }
        if (!result.output.isEmpty()) {
            return result.output;
        }
        return "ffprobe exited with code " + result.returnCode;
    }

    public static final class ProbeException extends Exception {
        public ProbeException(String message) {
            super(message);
        }
    }
}
