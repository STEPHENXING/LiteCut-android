package com.litecut.android.playback;

import android.net.Uri;

public interface PlaybackAdapter {
    void setMedia(Uri uri);

    void play();

    void pause();

    void seekTo(long positionMs);

    long currentPositionMs();

    long durationMs();

    boolean isPlaying();

    void release();
}
