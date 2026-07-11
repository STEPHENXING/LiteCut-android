package com.litecut.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MediaMetadata {
    public final String sourcePath;
    public final long durationMs;
    public final VideoStreamInfo videoStream;
    public final List<AudioStreamInfo> audioStreams;
    public final List<Long> keyframeMs;

    public MediaMetadata(
            String sourcePath,
            long durationMs,
            VideoStreamInfo videoStream,
            List<AudioStreamInfo> audioStreams,
            List<Long> keyframeMs
    ) {
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.durationMs = Math.max(0L, durationMs);
        this.videoStream = videoStream;
        this.audioStreams = Collections.unmodifiableList(new ArrayList<>(audioStreams));
        this.keyframeMs = Collections.unmodifiableList(new ArrayList<>(keyframeMs));
    }

    public boolean hasVideo() {
        return videoStream != null;
    }

    public boolean hasAudio() {
        return !audioStreams.isEmpty();
    }
}
