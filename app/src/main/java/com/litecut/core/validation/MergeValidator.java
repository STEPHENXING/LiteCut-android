package com.litecut.core.validation;

import com.litecut.core.model.MediaMetadata;
import com.litecut.core.model.VideoStreamInfo;
import com.litecut.core.plan.PlanException;

import java.util.List;

public final class MergeValidator {
    public void validate(List<MediaMetadata> items) throws PlanException {
        if (items == null || items.size() < 2) {
            throw new PlanException("At least two videos are required for merge.");
        }
        MediaMetadata first = items.get(0);
        if (first == null || !first.hasVideo()) {
            throw new PlanException("First merge item has no readable video stream.");
        }
        VideoStreamInfo reference = first.videoStream;
        for (int i = 1; i < items.size(); i++) {
            MediaMetadata current = items.get(i);
            if (current == null || !current.hasVideo()) {
                throw new PlanException("Merge item " + (i + 1) + " has no readable video stream.");
            }
            VideoStreamInfo video = current.videoStream;
            if (!reference.codecName.equals(video.codecName)) {
                throw new PlanException("Merge item " + (i + 1) + " codec mismatch: " + video.codecName);
            }
            if (reference.width != video.width || reference.height != video.height) {
                throw new PlanException("Merge item " + (i + 1) + " resolution mismatch: "
                        + video.width + "x" + video.height);
            }
            if (!reference.frameRate.sameValue(video.frameRate)) {
                throw new PlanException("Merge item " + (i + 1) + " frame-rate mismatch: " + video.frameRate);
            }
        }
    }
}
