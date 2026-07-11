package com.litecut.android.playback;

import android.content.Context;
import android.net.Uri;
import android.view.TextureView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public final class ExoPlayerPlaybackAdapter implements PlaybackAdapter {
    private final ExoPlayer player;
    private final TextureView textureView;
    private ErrorListener errorListener;

    public ExoPlayerPlaybackAdapter(Context context, TextureView textureView) {
        this.textureView = textureView;
        this.player = new ExoPlayer.Builder(context).build();
        this.player.setVideoTextureView(textureView);
        this.player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                if (errorListener != null) {
                    errorListener.onPlaybackError(error);
                }
            }
        });
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    @Override
    public void setMedia(Uri uri) {
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
    }

    @Override
    public void play() {
        player.play();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void seekTo(long positionMs) {
        player.seekTo(Math.max(0L, positionMs));
    }

    @Override
    public long currentPositionMs() {
        return Math.max(0L, player.getCurrentPosition());
    }

    @Override
    public long durationMs() {
        long duration = player.getDuration();
        return duration < 0L ? 0L : duration;
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying() || player.getPlayWhenReady();
    }

    @Override
    public void release() {
        player.clearVideoTextureView(textureView);
        player.release();
    }

    public interface ErrorListener {
        void onPlaybackError(Exception error);
    }
}
