package com.litecut.android.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.litecut.core.thumbnail.TimelineThumbnailRequest;
import com.litecut.core.timeline.MediaAsset;
import com.litecut.core.timeline.TimelineClip;
import com.litecut.core.timeline.TimelineProject;
import com.litecut.core.timeline.TimelineViewport;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TimelineView extends View {
    private static final float MIN_PX_PER_SECOND = 0.25f;
    private static final float DEFAULT_PX_PER_SECOND = 8f;
    private static final float MAX_PX_PER_SECOND = 36f;
    private static final float ZOOM_FACTOR = 2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final HashMap<String, Bitmap> bitmapCache = new HashMap<>();
    private TimelineProject project;
    private Map<String, List<TimelineThumbnailRequest>> thumbnailsByClipId = Collections.emptyMap();
    private OnClipSelectedListener listener;
    private float pxPerSecond;
    private float scrollX;
    private float downX;
    private float downY;
    private float downScrollX;
    private boolean dragging;
    private final int touchSlop;

    public TimelineView(Context context) {
        super(context);
        pxPerSecond = DEFAULT_PX_PER_SECOND;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setMinimumHeight(dp(112));
        setContentDescription("时间轴");
    }

    public void setProject(TimelineProject project) {
        this.project = project;
        clampScroll();
        invalidate();
    }

    public void setThumbnails(Map<String, List<TimelineThumbnailRequest>> thumbnailsByClipId) {
        this.thumbnailsByClipId = thumbnailsByClipId == null ? Collections.emptyMap() : thumbnailsByClipId;
        invalidate();
    }

    public void setOnClipSelectedListener(OnClipSelectedListener listener) {
        this.listener = listener;
    }

    public void zoomIn() {
        setPxPerSecond(pxPerSecond * ZOOM_FACTOR);
    }

    public void zoomOut() {
        setPxPerSecond(pxPerSecond / ZOOM_FACTOR);
    }

    public float pxPerSecond() {
        return pxPerSecond;
    }

    public float scrollXPosition() {
        return scrollX;
    }

    public float contentWidth() {
        return TimelineViewport.contentWidth(totalDurationMs(), pxPerSecond, innerWidth());
    }

    public boolean hasScrollableContent() {
        return contentWidth() > innerWidth() + 1f;
    }

    public void ensurePlayheadVisible() {
        if (project == null) {
            return;
        }
        float x = project.playheadMs() / 1000f * pxPerSecond;
        float left = scrollX;
        float right = scrollX + innerWidth();
        if (x < left) {
            scrollX = x;
        } else if (x > right) {
            scrollX = x - innerWidth() + dp(12);
        }
        clampScroll();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(245, 247, 250));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        if (project == null || project.clips().isEmpty()) {
            paint.setColor(Color.rgb(100, 116, 139));
            paint.setTextSize(sp(14));
            canvas.drawText("导入素材后显示时间轴", dp(16), getHeight() / 2f, paint);
            return;
        }

        float leftPadding = dp(12);
        float top = dp(20);
        float clipHeight = dp(58);
        int saved = canvas.save();
        canvas.clipRect(leftPadding, 0, getWidth() - dp(12), getHeight());

        long cursor = 0L;
        for (TimelineClip clip : project.clips()) {
            float left = TimelineViewport.xForTime(cursor, pxPerSecond, scrollX, leftPadding);
            float width = Math.max(dp(54), clip.durationMs() / 1000f * pxPerSecond);
            float right = left + width;
            if (right >= 0 && left <= getWidth()) {
                drawClip(canvas, clip, left, top, right, top + clipHeight);
            }
            cursor += clip.durationMs();
        }

        float playheadX = TimelineViewport.xForTime(project.playheadMs(), pxPerSecond, scrollX, leftPadding);
        paint.setColor(Color.rgb(220, 38, 38));
        canvas.drawRect(playheadX - dp(1), dp(8), playheadX + dp(1), getHeight() - dp(16), paint);
        canvas.restoreToCount(saved);
        drawScrollbar(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (project == null || project.clips().isEmpty()) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                downScrollX = scrollX;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true;
                    scrollX = downScrollX - dx;
                    clampScroll();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    selectAt(event.getX());
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    private void drawClip(Canvas canvas, TimelineClip clip, float left, float top, float right, float bottom) {
        boolean selected = clip.id.equals(project.selectedClipId());
        rect.set(left, top, right, bottom);
        paint.setColor(selected ? Color.rgb(20, 108, 90) : Color.rgb(71, 85, 105));
        canvas.drawRoundRect(rect, dp(6), dp(6), paint);
        drawThumbnails(canvas, clip, left, top, right, bottom);

        if (selected) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(250, 204, 21));
            canvas.drawRoundRect(rect, dp(6), dp(6), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        float width = right - left;
        paint.setColor(Color.WHITE);
        paint.setTextSize(sp(12));
        String title = clipLabel(clip);
        canvas.drawText(ellipsize(title, width - dp(14)), left + dp(8), top + dp(22), paint);
        if (width > dp(110)) {
            paint.setTextSize(sp(10));
            canvas.drawText(formatMs(clip.sourceInMs) + "-" + formatMs(clip.sourceOutMs), left + dp(8), bottom - dp(10), paint);
        }
    }

    private void drawThumbnails(Canvas canvas, TimelineClip clip, float left, float top, float right, float bottom) {
        List<TimelineThumbnailRequest> thumbnails = thumbnailsByClipId.get(clip.id);
        if (thumbnails == null || thumbnails.isEmpty()) {
            return;
        }
        float thumbWidth = (right - left) / thumbnails.size();
        for (int i = 0; i < thumbnails.size(); i++) {
            TimelineThumbnailRequest request = thumbnails.get(i);
            if (request.status != TimelineThumbnailRequest.Status.READY) {
                continue;
            }
            Bitmap bitmap = bitmapFor(request.outputFile);
            if (bitmap == null) {
                continue;
            }
            rect.set(left + i * thumbWidth, top, left + (i + 1) * thumbWidth, bottom);
            canvas.drawBitmap(bitmap, null, rect, paint);
        }
        paint.setColor(0x66000000);
        canvas.drawRoundRect(left, top, right, bottom, dp(6), dp(6), paint);
    }

    private void drawScrollbar(Canvas canvas) {
        float innerWidth = innerWidth();
        float contentWidth = contentWidth();
        if (contentWidth <= innerWidth + 1f) {
            return;
        }
        float trackLeft = dp(12);
        float trackTop = getHeight() - dp(9);
        float trackRight = getWidth() - dp(12);
        paint.setColor(Color.rgb(203, 213, 225));
        canvas.drawRoundRect(trackLeft, trackTop, trackRight, trackTop + dp(4), dp(2), dp(2), paint);
        float thumbWidth = Math.max(dp(28), innerWidth * innerWidth / contentWidth);
        float thumbLeft = trackLeft + (innerWidth - thumbWidth) * (scrollX / Math.max(1f, contentWidth - innerWidth));
        paint.setColor(Color.rgb(51, 65, 85));
        canvas.drawRoundRect(thumbLeft, trackTop, thumbLeft + thumbWidth, trackTop + dp(4), dp(2), dp(2), paint);
    }

    private void selectAt(float x) {
        long targetMs = TimelineViewport.timeForX(x, pxPerSecond, scrollX, dp(12), project.totalDurationMs());
        TimelineClip clip = project.clipAtTimelineMs(targetMs);
        if (clip != null) {
            project.selectClip(clip.id);
            project.setPlayheadMs(targetMs);
            ensurePlayheadVisible();
            if (listener != null) {
                listener.onClipSelected(clip.id, targetMs);
            }
            invalidate();
        }
    }

    private void setPxPerSecond(float nextPxPerSecond) {
        float playheadContentX = project == null ? 0f : project.playheadMs() / 1000f * pxPerSecond;
        float next = Math.max(MIN_PX_PER_SECOND, Math.min(MAX_PX_PER_SECOND, nextPxPerSecond));
        float ratio = next / pxPerSecond;
        pxPerSecond = next;
        scrollX = playheadContentX * ratio - innerWidth() / 2f;
        clampScroll();
        invalidate();
    }

    private void clampScroll() {
        scrollX = TimelineViewport.clampScroll(scrollX, contentWidth(), innerWidth());
    }

    private long totalDurationMs() {
        return project == null ? 0L : project.totalDurationMs();
    }

    private float innerWidth() {
        return Math.max(dp(80), getWidth() - dp(24));
    }

    private Bitmap bitmapFor(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        String path = file.getAbsolutePath();
        Bitmap cached = bitmapCache.get(path);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            bitmapCache.put(path, bitmap);
        }
        return bitmap;
    }

    private String clipLabel(TimelineClip clip) {
        MediaAsset asset = project.assetForClip(clip);
        return asset.displayName == null ? "片段" : asset.displayName;
    }

    private String ellipsize(String text, float maxWidth) {
        if (text == null || maxWidth <= 0f || paint.measureText(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static String formatMs(long value) {
        long totalSeconds = Math.max(0L, value) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.CHINA, "%02d:%02d", minutes, seconds);
    }

    public interface OnClipSelectedListener {
        void onClipSelected(String clipId, long playheadMs);
    }
}
