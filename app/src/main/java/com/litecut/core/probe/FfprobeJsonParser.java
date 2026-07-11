package com.litecut.core.probe;

import com.litecut.core.model.AudioStreamInfo;
import com.litecut.core.model.Fraction;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.model.VideoStreamInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class FfprobeJsonParser {
    public MediaMetadata parse(String sourcePath, String metadataJson, String keyframeJson) throws JSONException {
        MediaMetadata metadata = parseMetadata(sourcePath, metadataJson);
        return new MediaMetadata(
                metadata.sourcePath,
                metadata.durationMs,
                metadata.videoStream,
                metadata.audioStreams,
                parseKeyframes(keyframeJson)
        );
    }

    public MediaMetadata parseMetadata(String sourcePath, String metadataJson) throws JSONException {
        JSONObject root = new JSONObject(metadataJson);
        JSONObject format = root.optJSONObject("format");
        long durationMs = Math.round((format == null ? 0.0 : format.optDouble("duration", 0.0)) * 1000.0);
        JSONArray streams = root.optJSONArray("streams");

        VideoStreamInfo video = null;
        ArrayList<AudioStreamInfo> audio = new ArrayList<>();
        if (streams != null) {
            for (int i = 0; i < streams.length(); i++) {
                JSONObject stream = streams.getJSONObject(i);
                String type = stream.optString("codec_type");
                if ("video".equals(type) && video == null) {
                    video = new VideoStreamInfo(
                            stream.optInt("index"),
                            stream.optString("codec_name"),
                            stream.optInt("width"),
                            stream.optInt("height"),
                            Fraction.parse(stream.optString("r_frame_rate"))
                    );
                } else if ("audio".equals(type)) {
                    audio.add(new AudioStreamInfo(stream.optInt("index"), stream.optString("codec_name")));
                }
            }
        }

        return new MediaMetadata(sourcePath, durationMs, video, audio, java.util.Collections.emptyList());
    }

    public List<Long> parseKeyframes(String keyframeJson) throws JSONException {
        ArrayList<Long> result = new ArrayList<>();
        if (keyframeJson == null || keyframeJson.trim().isEmpty()) {
            return result;
        }
        JSONObject root = new JSONObject(keyframeJson);
        JSONArray frames = root.optJSONArray("frames");
        if (frames == null) {
            return result;
        }
        for (int i = 0; i < frames.length(); i++) {
            JSONObject frame = frames.getJSONObject(i);
            if (frame.optInt("key_frame", 0) != 1) {
                continue;
            }
            String time = frame.optString("best_effort_timestamp_time", frame.optString("pkt_pts_time", ""));
            if (!time.isEmpty() && !"N/A".equals(time)) {
                result.add(Math.round(Double.parseDouble(time) * 1000.0));
            }
        }
        return result;
    }
}
