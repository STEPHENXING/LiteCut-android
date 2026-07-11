package com.litecut.android.playback;

import android.net.Uri;
import android.widget.VideoView;

public final class VideoViewPlaybackAdapter implements PlaybackAdapter {
    private final VideoView videoView;

    public VideoViewPlaybackAdapter(VideoView videoView) {
        this.videoView = videoView;
    }

    @Override
    public void setMedia(Uri uri) {
        videoView.setVideoURI(uri);
    }

    @Override
    public void play() {
        videoView.start();
    }

    @Override
    public void pause() {
        videoView.pause();
    }

    @Override
    public void seekTo(long positionMs) {
        videoView.seekTo((int) Math.max(0L, Math.min(Integer.MAX_VALUE, positionMs)));
    }

    @Override
    public long currentPositionMs() {
        return Math.max(0, videoView.getCurrentPosition());
    }

    @Override
    public long durationMs() {
        int duration = videoView.getDuration();
        return duration < 0 ? 0L : duration;
    }

    @Override
    public boolean isPlaying() {
        return videoView.isPlaying();
    }

    @Override
    public void release() {
        videoView.stopPlayback();
    }
}
