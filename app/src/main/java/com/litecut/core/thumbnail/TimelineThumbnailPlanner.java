package com.litecut.core.thumbnail;

import com.litecut.core.timeline.TimelineClip;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class TimelineThumbnailPlanner {
    public static final int MAX_THUMBNAILS_PER_CLIP = 8;

    public List<TimelineThumbnailRequest> planForClip(TimelineClip clip, String assetId, File cacheDir) {
        ArrayList<TimelineThumbnailRequest> requests = new ArrayList<>();
        long durationMs = clip.durationMs();
        if (durationMs <= 0L) {
            return requests;
        }
        int count = thumbnailCount(durationMs);
        for (int i = 0; i < count; i++) {
            long offset = count == 1 ? durationMs / 2L : (durationMs * i) / count;
            long sourceTimeMs = clip.sourceInMs + Math.min(durationMs - 1L, Math.max(0L, offset));
            File output = new File(cacheDir, TimelineThumbnailRequest.cacheKey(assetId, sourceTimeMs));
            requests.add(new TimelineThumbnailRequest(assetId, sourceTimeMs, output, TimelineThumbnailRequest.Status.PENDING));
        }
        return requests;
    }

    public int thumbnailCount(long durationMs) {
        long seconds = Math.max(1L, durationMs / 1000L);
        if (seconds <= 8L) {
            return 1;
        }
        if (seconds <= 30L) {
            return 2;
        }
        if (seconds <= 120L) {
            return 4;
        }
        return MAX_THUMBNAILS_PER_CLIP;
    }
}
