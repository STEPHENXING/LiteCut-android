package com.litecut.core.plan;

import com.litecut.core.model.EditRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GopRangeExpander {
    public EditRange expand(long selectedInMs, long selectedOutMs, long durationMs, List<Long> keyframesMs)
            throws PlanException {
        if (durationMs <= 0L) {
            throw new PlanException("Video duration is missing.");
        }
        long safeIn = clamp(selectedInMs, 0L, durationMs);
        long safeOut = clamp(selectedOutMs, 0L, durationMs);
        if (safeOut <= safeIn) {
            throw new PlanException("Out point must be after In point.");
        }

        List<Long> keyframes = normalizedKeyframes(keyframesMs, durationMs);
        long effectiveIn = 0L;
        long effectiveOut = durationMs;

        for (Long keyframe : keyframes) {
            if (keyframe <= safeIn) {
                effectiveIn = keyframe;
            }
            if (keyframe > safeOut) {
                effectiveOut = keyframe;
                break;
            }
        }

        return new EditRange(safeIn, safeOut, effectiveIn, effectiveOut);
    }

    private static List<Long> normalizedKeyframes(List<Long> keyframesMs, long durationMs) {
        ArrayList<Long> values = new ArrayList<>();
        values.add(0L);
        if (keyframesMs != null) {
            for (Long keyframe : keyframesMs) {
                if (keyframe != null && keyframe >= 0L && keyframe <= durationMs) {
                    values.add(keyframe);
                }
            }
        }
        values.add(durationMs);
        Collections.sort(values);

        ArrayList<Long> unique = new ArrayList<>();
        long previous = Long.MIN_VALUE;
        for (Long value : values) {
            if (value != previous) {
                unique.add(value);
                previous = value;
            }
        }
        return unique;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
