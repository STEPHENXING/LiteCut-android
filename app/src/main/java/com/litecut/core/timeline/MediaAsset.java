package com.litecut.core.timeline;

import com.litecut.core.model.MediaMetadata;

import java.io.File;

public final class MediaAsset {
    public final String id;
    public final String sourceUri;
    public final String displayName;
    public long durationMs;
    public MediaMetadata metadata;
    public File localWorkspaceFile;
    public AnalysisState analysisState;
    public String errorMessage;

    public MediaAsset(String id, String sourceUri, String displayName, long durationMs) {
        this.id = requireValue(id, "id");
        this.sourceUri = requireValue(sourceUri, "sourceUri");
        this.displayName = displayName == null || displayName.trim().isEmpty() ? "未命名素材" : displayName;
        this.durationMs = Math.max(0L, durationMs);
        this.analysisState = AnalysisState.SELECTED;
    }

    public boolean hasLocalFile() {
        return localWorkspaceFile != null && localWorkspaceFile.exists();
    }

    public boolean hasMetadata() {
        return metadata != null && metadata.hasVideo();
    }

    public boolean hasKeyframes() {
        return metadata != null && !metadata.keyframeMs.isEmpty();
    }

    public void markCopying() {
        analysisState = AnalysisState.COPYING;
        errorMessage = null;
    }

    public void markLocalReady(File file) {
        localWorkspaceFile = file;
        analysisState = AnalysisState.LOCAL_READY;
        errorMessage = null;
    }

    public void markMetadataReady(MediaMetadata mediaMetadata) {
        metadata = mediaMetadata;
        durationMs = mediaMetadata.durationMs;
        analysisState = mediaMetadata.keyframeMs.isEmpty()
                ? AnalysisState.METADATA_READY
                : AnalysisState.KEYFRAMES_READY;
        errorMessage = null;
    }

    public void markFailed(String message) {
        analysisState = AnalysisState.FAILED;
        errorMessage = message == null ? "分析失败" : message;
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value;
    }
}
