package com.litecut.core.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TimelineProject {
    private final LinkedHashMap<String, MediaAsset> assets = new LinkedHashMap<>();
    private final ArrayList<TimelineClip> clips = new ArrayList<>();
    private String selectedClipId;
    private long playheadMs;

    public MediaAsset appendAsset(String sourceUri, String displayName, long durationMs) {
        MediaAsset asset = new MediaAsset(newId("asset"), sourceUri, displayName, durationMs);
        assets.put(asset.id, asset);
        TimelineClip clip = new TimelineClip(newId("clip"), asset.id, 0L, Math.max(0L, durationMs));
        clips.add(clip);
        selectedClipId = clip.id;
        return asset;
    }

    public void updateAssetMetadata(String assetId, long durationMs) {
        MediaAsset asset = requireAsset(assetId);
        asset.durationMs = Math.max(0L, durationMs);
        for (int i = 0; i < clips.size(); i++) {
            TimelineClip clip = clips.get(i);
            if (clip.assetId.equals(assetId) && clip.durationMs() == 0L) {
                clips.set(i, new TimelineClip(clip.id, clip.assetId, 0L, asset.durationMs));
            }
        }
    }

    public void selectClip(String clipId) {
        requireClipIndex(clipId);
        selectedClipId = clipId;
    }

    public TimelineClip selectedClip() {
        int index = findClipIndex(selectedClipId);
        return index < 0 ? null : clips.get(index);
    }

    public void setPlayheadMs(long playheadMs) {
        this.playheadMs = clamp(playheadMs, 0L, totalDurationMs());
    }

    public long playheadMs() {
        return playheadMs;
    }

    public long totalDurationMs() {
        long total = 0L;
        for (TimelineClip clip : clips) {
            total += clip.durationMs();
        }
        return total;
    }

    public TimelineClip clipAtTimelineMs(long timelineMs) {
        TimelineMapping mapping = mapTimelineToSource(timelineMs);
        return mapping == null ? null : clips.get(requireClipIndex(mapping.clipId));
    }

    public TimelineMapping mapTimelineToSource(long timelineMs) {
        if (clips.isEmpty()) {
            return null;
        }
        long total = totalDurationMs();
        long safeTimelineMs = clamp(timelineMs, 0L, total);
        long cursor = 0L;
        for (TimelineClip clip : clips) {
            long duration = clip.durationMs();
            long end = cursor + duration;
            if (safeTimelineMs < end || clip == clips.get(clips.size() - 1)) {
                long localOffset = clamp(safeTimelineMs - cursor, 0L, duration);
                long sourceTime = clamp(clip.sourceInMs + localOffset, clip.sourceInMs, clip.sourceOutMs);
                long resolvedTimeline = cursor + localOffset;
                return new TimelineMapping(clip.id, localOffset, sourceTime, resolvedTimeline);
            }
            cursor = end;
        }
        TimelineClip last = clips.get(clips.size() - 1);
        long localOffset = last.durationMs();
        return new TimelineMapping(last.id, localOffset, last.sourceOutMs, total);
    }

    public TimelineMapping mapSourceToTimeline(String clipId, long sourceTimeMs) {
        int index = requireClipIndex(clipId);
        TimelineClip clip = clips.get(index);
        long sourceTime = clamp(sourceTimeMs, clip.sourceInMs, clip.sourceOutMs);
        long localOffset = clamp(sourceTime - clip.sourceInMs, 0L, clip.durationMs());
        return new TimelineMapping(clip.id, localOffset, sourceTime, timelineStartForClip(clip.id) + localOffset);
    }

    public void splitSelectedAtPlayhead() {
        TimelineClip clip = selectedClip();
        if (clip == null) {
            return;
        }
        long clipStart = timelineStartForClip(clip.id);
        long offset = playheadMs - clipStart;
        if (offset <= 0L || offset >= clip.durationMs()) {
            return;
        }
        int index = requireClipIndex(clip.id);
        long splitSourceMs = clip.sourceInMs + offset;
        TimelineClip before = clip.withSourceRange(clip.id, clip.sourceInMs, splitSourceMs);
        TimelineClip after = clip.withSourceRange(newId("clip"), splitSourceMs, clip.sourceOutMs);
        clips.set(index, before);
        clips.add(index + 1, after);
        selectedClipId = after.id;
    }

    public void duplicateSelectedClip() {
        TimelineClip clip = selectedClip();
        if (clip == null) {
            return;
        }
        int index = requireClipIndex(clip.id);
        TimelineClip copy = clip.copyAs(newId("clip"));
        clips.add(index + 1, copy);
        selectedClipId = copy.id;
    }

    public void deleteSelectedClip() {
        int index = findClipIndex(selectedClipId);
        if (index < 0) {
            return;
        }
        clips.remove(index);
        if (clips.isEmpty()) {
            selectedClipId = null;
            setPlayheadMs(0L);
            return;
        }
        int next = Math.min(index, clips.size() - 1);
        selectedClipId = clips.get(next).id;
        setPlayheadMs(playheadMs);
    }

    public void moveClip(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= clips.size()) {
            return;
        }
        int safeTo = (int) clamp(toIndex, 0, clips.size() - 1);
        if (fromIndex == safeTo) {
            return;
        }
        TimelineClip moved = clips.remove(fromIndex);
        clips.add(safeTo, moved);
        selectedClipId = moved.id;
    }

    public long timelineStartForClip(String clipId) {
        long cursor = 0L;
        for (TimelineClip clip : clips) {
            if (clip.id.equals(clipId)) {
                return cursor;
            }
            cursor += clip.durationMs();
        }
        return 0L;
    }

    public Map<String, MediaAsset> assets() {
        return Collections.unmodifiableMap(assets);
    }

    public List<TimelineClip> clips() {
        return Collections.unmodifiableList(clips);
    }

    public MediaAsset assetForClip(TimelineClip clip) {
        return requireAsset(clip.assetId);
    }

    public String selectedClipId() {
        return selectedClipId;
    }

    private MediaAsset requireAsset(String assetId) {
        MediaAsset asset = assets.get(assetId);
        if (asset == null) {
            throw new IllegalArgumentException("Unknown asset: " + assetId);
        }
        return asset;
    }

    private int requireClipIndex(String clipId) {
        int index = findClipIndex(clipId);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown clip: " + clipId);
        }
        return index;
    }

    private int findClipIndex(String clipId) {
        if (clipId == null) {
            return -1;
        }
        for (int i = 0; i < clips.size(); i++) {
            if (clipId.equals(clips.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
