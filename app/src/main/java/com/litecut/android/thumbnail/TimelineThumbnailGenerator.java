package com.litecut.android.thumbnail;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import com.litecut.core.thumbnail.TimelineThumbnailRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class TimelineThumbnailGenerator {
    public TimelineThumbnailRequest generate(File mediaFile, TimelineThumbnailRequest request) {
        if (request.outputFile.exists() && request.outputFile.length() > 0L) {
            return request.withStatus(TimelineThumbnailRequest.Status.READY);
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mediaFile.getAbsolutePath());
            Bitmap bitmap = retriever.getFrameAtTime(request.sourceTimeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (bitmap == null) {
                return request.withStatus(TimelineThumbnailRequest.Status.FAILED);
            }
            File parent = request.outputFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream output = new FileOutputStream(request.outputFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 72, output);
            }
            bitmap.recycle();
            return request.withStatus(TimelineThumbnailRequest.Status.READY);
        } catch (RuntimeException | IOException exception) {
            return request.withStatus(TimelineThumbnailRequest.Status.FAILED);
        } finally {
            try {
                retriever.release();
            } catch (IOException ignored) {
                // Releasing a retriever should not turn thumbnail fallback into an app error.
            }
        }
    }
}
