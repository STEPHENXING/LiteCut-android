package com.litecut;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.litecut.android.AndroidMediaAccess;
import com.litecut.android.FfmpegExecutor;
import com.litecut.android.FfmpegKitExecutor;
import com.litecut.android.FfmpegResult;
import com.litecut.android.MediaProbeRepository;
import com.litecut.android.OperationRunner;
import com.litecut.android.playback.ExoPlayerPlaybackAdapter;
import com.litecut.android.playback.PlaybackAdapter;
import com.litecut.android.thumbnail.TimelineThumbnailCache;
import com.litecut.android.thumbnail.TimelineThumbnailGenerator;
import com.litecut.android.ui.TimelineView;
import com.litecut.core.model.EditRange;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.plan.FfmpegCommand;
import com.litecut.core.plan.OperationPlanner;
import com.litecut.core.plan.OutputNamer;
import com.litecut.core.plan.PlanException;
import com.litecut.core.plan.TimelineExportPlanner;
import com.litecut.core.thumbnail.TimelineThumbnailPlanner;
import com.litecut.core.thumbnail.TimelineThumbnailRequest;
import com.litecut.core.timeline.MediaAsset;
import com.litecut.core.timeline.TimelineClip;
import com.litecut.core.timeline.TimelineMapping;
import com.litecut.core.timeline.TimelineProject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_IMPORT = 1001;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final AndroidMediaAccess mediaAccess = new AndroidMediaAccess();
    private final TimelineProject project = new TimelineProject();
    private final OperationPlanner operationPlanner = new OperationPlanner();
    private final TimelineExportPlanner timelineExportPlanner = new TimelineExportPlanner();
    private final OutputNamer outputNamer = new OutputNamer();

    private FfmpegExecutor ffmpegExecutor;
    private MediaProbeRepository probeRepository;
    private OperationRunner operationRunner;
    private PlaybackAdapter playback;
    private TimelineThumbnailCache thumbnailCache;
    private final TimelineThumbnailPlanner thumbnailPlanner = new TimelineThumbnailPlanner();
    private final TimelineThumbnailGenerator thumbnailGenerator = new TimelineThumbnailGenerator();
    private final HashSet<String> thumbnailGenerationKeys = new HashSet<>();
    private Map<String, List<TimelineThumbnailRequest>> thumbnailRequestsByClipId = new HashMap<>();

    private TextureView previewTexture;
    private Button playPauseButton;
    private SeekBar previewSeek;
    private TimelineView timelineView;
    private ProgressBar exportProgress;
    private TextView timeView;
    private TextView statusView;
    private TextView assetView;
    private TextView rangeView;

    private String loadedPreviewAssetId;
    private boolean userSeeking;

    private final Runnable playbackTicker = new Runnable() {
        @Override
        public void run() {
            syncPlayheadFromPlayback();
            updateEditorText();
            uiHandler.postDelayed(this, 300L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ffmpegExecutor = new FfmpegKitExecutor();
        probeRepository = new MediaProbeRepository(ffmpegExecutor);
        operationRunner = new OperationRunner(ffmpegExecutor);
        thumbnailCache = new TimelineThumbnailCache(this);
        mediaAccess.cleanupTemp(this);
        setContentView(createEditorUi());
        uiHandler.post(playbackTicker);
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(playbackTicker);
        if (playback != null) {
            playback.release();
        }
        background.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data == null) {
            return;
        }
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                importUri(data.getClipData().getItemAt(i).getUri(), data.getFlags(), i == 0);
            }
        } else if (data.getData() != null) {
            importUri(data.getData(), data.getFlags(), true);
        }
    }

    private View createEditorUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.setBackgroundColor(0xFFF8FAFC);

        TextView title = label("LiteCut 轻剪辑", 18, 0xFF0F172A);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, matchWrap());

        FrameLayout previewShell = new FrameLayout(this);
        previewShell.setBackgroundColor(0xFF111827);
        previewShell.setContentDescription("预览");
        previewTexture = new TextureView(this);
        previewTexture.setOpaque(true);
        previewShell.addView(previewTexture, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(previewShell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230)));
        ExoPlayerPlaybackAdapter exoPlayback = new ExoPlayerPlaybackAdapter(this, previewTexture);
        exoPlayback.setErrorListener(error -> showError("预览播放失败：" + previewErrorMessage(error)));
        playback = exoPlayback;

        LinearLayout playbackRow = row();
        playPauseButton = compactButton("播放");
        playPauseButton.setOnClickListener(v -> togglePlayback());
        playbackRow.addView(playPauseButton, wrap());
        timeView = label("00:00 / 00:00", 13, 0xFF334155);
        timeView.setGravity(Gravity.CENTER_VERTICAL);
        playbackRow.addView(timeView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(playbackRow);

        previewSeek = new SeekBar(this);
        previewSeek.setMax(1);
        previewSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekTimeline(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
            }
        });
        root.addView(previewSeek, matchWrap());

        LinearLayout tools = row();
        tools.setGravity(Gravity.CENTER_VERTICAL);
        Button importButton = compactButton("导入");
        importButton.setOnClickListener(v -> startActivityForResult(mediaAccess.videoOpenIntent(true), REQUEST_IMPORT));
        tools.addView(importButton, toolWeight());
        tools.addView(toolButton("切割", v -> splitSelected()), toolWeight());
        tools.addView(toolButton("复制", v -> duplicateSelected()), toolWeight());
        tools.addView(toolButton("删除", v -> deleteSelected()), toolWeight());
        tools.addView(toolButton("前移", v -> moveSelected(-1)), toolWeight());
        tools.addView(toolButton("后移", v -> moveSelected(1)), toolWeight());
        tools.addView(toolButton("导出", v -> exportTimeline()), toolWeight());
        root.addView(tools, matchWrap());

        assetView = label("尚未导入素材", 13, 0xFF475569);
        root.addView(assetView, matchWrap());

        LinearLayout timelineTools = row();
        timelineTools.setGravity(Gravity.CENTER_VERTICAL);
        TextView timelineLabel = label("时间轴", 13, 0xFF334155);
        timelineTools.addView(timelineLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button zoomOut = compactButton("缩小");
        zoomOut.setContentDescription("缩小时间轴");
        zoomOut.setOnClickListener(v -> {
            timelineView.zoomOut();
            setStatus("时间轴已缩小。");
        });
        Button zoomIn = compactButton("放大");
        zoomIn.setContentDescription("放大时间轴");
        zoomIn.setOnClickListener(v -> {
            timelineView.zoomIn();
            setStatus("时间轴已放大。");
        });
        timelineTools.addView(zoomOut, wrap());
        timelineTools.addView(zoomIn, wrap());
        root.addView(timelineTools, matchWrap());

        timelineView = new TimelineView(this);
        timelineView.setProject(project);
        timelineView.setOnClipSelectedListener((clipId, playheadMs) -> {
            project.selectClip(clipId);
            project.setPlayheadMs(playheadMs);
            loadSelectedClipPreview(false);
            updateEditorText();
        });
        root.addView(timelineView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(104)));

        rangeView = label("有效范围：等待选择片段", 13, 0xFF334155);
        root.addView(rangeView, matchWrap());

        exportProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        exportProgress.setMax(100);
        root.addView(exportProgress, matchWrap());

        statusView = label("就绪。导入素材后可立即预览，后台会准备导出数据。", 13, 0xFF146C5A);
        root.addView(statusView, matchWrap());

        return root;
    }

    private void importUri(Uri uri, int flags, boolean previewImmediately) {
        mediaAccess.persistReadPermission(this, uri, flags);
        String displayName = mediaAccess.displayName(this, uri, "素材.mp4");
        MediaAsset asset = project.appendAsset(uri.toString(), displayName, 0L);
        if (previewImmediately) {
            project.selectClip(project.clips().get(project.clips().size() - 1).id);
            playback.setMedia(uri);
            playback.play();
            loadedPreviewAssetId = asset.id;
        }
        setStatus("已添加素材：" + displayName + "。正在后台准备分析。");
        refreshTimeline();

        background.execute(() -> {
            try {
                asset.markCopying();
                postStatus("正在复制素材用于导出：" + asset.displayName);
                File local = mediaAccess.copyToWorkspace(this, uri, displayName);
                asset.markLocalReady(local);
                postStatus("正在读取基础信息：" + asset.displayName);
                MediaMetadata basic = probeRepository.probeBasic(local);
                asset.markMetadataReady(basic);
                project.updateAssetMetadata(asset.id, basic.durationMs);
                runOnUiThread(() -> {
                    refreshTimeline();
                    setStatus("基础信息完成，正在分析关键帧：" + asset.displayName);
                });
                scheduleThumbnailGenerationForReadyAssets();
                MediaMetadata withKeyframes = probeRepository.probeKeyframes(local, basic);
                asset.markMetadataReady(withKeyframes);
                runOnUiThread(() -> {
                    refreshTimeline();
                    setStatus("关键帧分析完成：" + asset.displayName);
                });
            } catch (Exception exception) {
                asset.markFailed(exception.getMessage());
                showError("素材分析失败：" + asset.displayName + "，" + exception.getMessage());
            }
        });
    }

    private void togglePlayback() {
        if (project.clips().isEmpty()) {
            setStatus("请先导入素材。");
            return;
        }
        if (playback.isPlaying()) {
            playback.pause();
        } else {
            loadSelectedClipPreview(false);
            playback.play();
        }
        updateEditorText();
    }

    private void seekTimeline(long timelineMs) {
        TimelineMapping mapping = project.mapTimelineToSource(timelineMs);
        if (mapping != null) {
            project.selectClip(mapping.clipId);
            project.setPlayheadMs(mapping.timelineTimeMs);
            loadSelectedClipPreview(false);
        } else {
            project.setPlayheadMs(timelineMs);
        }
        refreshTimeline();
    }

    private void splitSelected() {
        project.splitSelectedAtPlayhead();
        refreshTimeline();
        scheduleThumbnailGenerationForReadyAssets();
        setStatus("已在播放头位置切割片段。");
    }

    private void duplicateSelected() {
        project.duplicateSelectedClip();
        refreshTimeline();
        scheduleThumbnailGenerationForReadyAssets();
        setStatus("已复制选中片段。");
    }

    private void deleteSelected() {
        project.deleteSelectedClip();
        refreshTimeline();
        setStatus("已删除选中片段，源素材不会被删除。");
    }

    private void moveSelected(int delta) {
        int index = selectedClipIndex();
        if (index < 0) {
            setStatus("请先选择片段。");
            return;
        }
        project.moveClip(index, index + delta);
        refreshTimeline();
        timelineView.ensurePlayheadVisible();
        setStatus("已调整片段顺序。");
    }

    private void exportTimeline() {
        if (project.clips().isEmpty()) {
            setStatus("时间轴为空，无法导出。");
            return;
        }
        setStatus("正在准备导出...");
        exportProgress.setProgress(0);
        background.execute(() -> {
            try {
                for (MediaAsset asset : project.assets().values()) {
                    prepareAssetForExport(asset);
                }
                File tempDir = mediaAccess.tempDir(this);
                if (!tempDir.exists() && !tempDir.mkdirs()) {
                    throw new IllegalStateException("无法创建临时目录。");
                }
                File first = firstLocalFile();
                File output = outputNamer.defaultOutput(first, mediaAccess.workspaceDir(this), "_timeline", ".mp4");
                List<FfmpegCommand> commands = timelineExportPlanner.planTimelineExport(project, tempDir, output);
                postStatus("正在无损导出时间轴...");
                FfmpegResult result = operationRunner.run(commands, estimateExpectedBytes(),
                        percent -> runOnUiThread(() -> exportProgress.setProgress(percent)));
                operationRunner.cleanupTemporaryFiles(commands);
                if (!result.success) {
                    throw new IllegalStateException(result.error.isEmpty() ? result.output : result.error);
                }
                Uri published = mediaAccess.publishVideo(this, output, output.getName());
                runOnUiThread(() -> {
                    exportProgress.setProgress(100);
                    setStatus("导出完成：" + published);
                });
            } catch (Exception exception) {
                showError("导出失败：" + exception.getMessage());
            }
        });
    }

    private void prepareAssetForExport(MediaAsset asset) throws Exception {
        if (!asset.hasLocalFile()) {
            postStatus("正在准备导出文件：" + asset.displayName);
            Uri uri = Uri.parse(asset.sourceUri);
            File local = mediaAccess.copyToWorkspace(this, uri, asset.displayName);
            asset.markLocalReady(local);
        }
        if (!asset.hasMetadata()) {
            postStatus("正在读取基础信息：" + asset.displayName);
            asset.markMetadataReady(probeRepository.probeBasic(asset.localWorkspaceFile));
            project.updateAssetMetadata(asset.id, asset.durationMs);
        }
        if (!asset.hasKeyframes()) {
            postStatus("正在分析关键帧：" + asset.displayName);
            asset.markMetadataReady(probeRepository.probeKeyframes(asset.localWorkspaceFile, asset.metadata));
        }
    }

    private void loadSelectedClipPreview(boolean autoPlay) {
        TimelineClip clip = project.selectedClip();
        if (clip == null) {
            return;
        }
        TimelineMapping mapping = project.mapTimelineToSource(project.playheadMs());
        if (mapping == null) {
            return;
        }
        if (!mapping.clipId.equals(clip.id)) {
            project.selectClip(mapping.clipId);
            clip = project.selectedClip();
        }
        MediaAsset asset = project.assetForClip(clip);
        if (!asset.id.equals(loadedPreviewAssetId)) {
            playback.setMedia(Uri.parse(asset.sourceUri));
            loadedPreviewAssetId = asset.id;
        }
        playback.seekTo(mapping.sourceTimeMs);
        if (autoPlay) {
            playback.play();
        }
    }

    private void syncPlayheadFromPlayback() {
        TimelineClip clip = project.selectedClip();
        if (clip == null || !clip.assetId.equals(loadedPreviewAssetId)) {
            return;
        }
        long sourcePosition = playback.currentPositionMs();
        TimelineMapping mapping = project.mapSourceToTimeline(clip.id, sourcePosition);
        if (sourcePosition >= clip.sourceOutMs && clip.durationMs() > 0L) {
            playback.pause();
        }
        project.setPlayheadMs(mapping.timelineTimeMs);
        if (!userSeeking) {
            previewSeek.setMax((int) Math.min(Integer.MAX_VALUE, Math.max(1L, project.totalDurationMs())));
            previewSeek.setProgress((int) Math.min(Integer.MAX_VALUE, project.playheadMs()));
        }
        timelineView.invalidate();
    }

    private void refreshTimeline() {
        refreshThumbnailRequests();
        timelineView.setProject(project);
        timelineView.setThumbnails(thumbnailRequestsByClipId);
        previewSeek.setMax((int) Math.min(Integer.MAX_VALUE, Math.max(1L, project.totalDurationMs())));
        updateEditorText();
    }

    private void refreshThumbnailRequests() {
        HashMap<String, List<TimelineThumbnailRequest>> next = new HashMap<>();
        if (thumbnailCache == null) {
            thumbnailRequestsByClipId = next;
            return;
        }
        for (TimelineClip clip : project.clips()) {
            MediaAsset asset = project.assetForClip(clip);
            if (!asset.hasLocalFile()) {
                continue;
            }
            ArrayList<TimelineThumbnailRequest> requests = new ArrayList<>();
            for (TimelineThumbnailRequest request : thumbnailPlanner.planForClip(clip, asset.id, thumbnailCache.directory())) {
                TimelineThumbnailRequest.Status status = request.outputFile.exists()
                        ? TimelineThumbnailRequest.Status.READY
                        : TimelineThumbnailRequest.Status.PENDING;
                requests.add(request.withStatus(status));
            }
            next.put(clip.id, requests);
        }
        thumbnailRequestsByClipId = next;
    }

    private void scheduleThumbnailGenerationForReadyAssets() {
        if (thumbnailCache == null) {
            return;
        }
        ArrayList<ThumbnailJob> jobs = new ArrayList<>();
        for (TimelineClip clip : project.clips()) {
            MediaAsset asset = project.assetForClip(clip);
            if (!asset.hasLocalFile()) {
                continue;
            }
            for (TimelineThumbnailRequest request : thumbnailPlanner.planForClip(clip, asset.id, thumbnailCache.directory())) {
                String key = request.outputFile.getAbsolutePath();
                if (!request.outputFile.exists() && thumbnailGenerationKeys.add(key)) {
                    jobs.add(new ThumbnailJob(asset.localWorkspaceFile, request));
                }
            }
        }
        if (jobs.isEmpty()) {
            return;
        }
        background.execute(() -> {
            for (ThumbnailJob job : jobs) {
                thumbnailGenerator.generate(job.mediaFile, job.request);
            }
            runOnUiThread(this::refreshTimeline);
        });
    }

    private void updateEditorText() {
        playPauseButton.setText(playback != null && playback.isPlaying() ? "暂停" : "播放");
        timeView.setText(formatMs(project.playheadMs()) + " / " + formatMs(project.totalDurationMs()));

        TimelineClip selected = project.selectedClip();
        if (selected == null) {
            assetView.setText("尚未导入素材");
            rangeView.setText("有效范围：等待选择片段");
            return;
        }
        MediaAsset asset = project.assetForClip(selected);
        assetView.setText("选中：" + asset.displayName
                + "｜片段 " + formatMs(selected.sourceInMs) + " - " + formatMs(selected.sourceOutMs)
                + "｜状态：" + chineseState(asset));
        if (!asset.hasMetadata()) {
            rangeView.setText("有效范围：正在读取素材信息");
        } else if (!asset.hasKeyframes()) {
            rangeView.setText("有效范围：正在分析关键帧，导出前会自动对齐");
        } else {
            try {
                EditRange range = operationPlanner.expandRange(asset.metadata, selected.sourceInMs, selected.sourceOutMs);
                rangeView.setText("有效范围：" + formatMs(range.effectiveInMs) + " - " + formatMs(range.effectiveOutMs)
                        + "（无损快速导出，边界会自动对齐关键帧）");
            } catch (PlanException exception) {
                rangeView.setText("有效范围：" + exception.getMessage());
            }
        }
    }

    private File firstLocalFile() throws PlanException {
        for (MediaAsset asset : project.assets().values()) {
            if (asset.localWorkspaceFile != null) {
                return asset.localWorkspaceFile;
            }
        }
        throw new PlanException("没有可导出的本地素材。");
    }

    private long estimateExpectedBytes() {
        long total = 0L;
        for (MediaAsset asset : project.assets().values()) {
            if (asset.localWorkspaceFile != null && asset.localWorkspaceFile.exists()) {
                total += asset.localWorkspaceFile.length();
            }
        }
        return Math.max(1L, total);
    }

    private int selectedClipIndex() {
        String selectedId = project.selectedClipId();
        for (int i = 0; i < project.clips().size(); i++) {
            if (project.clips().get(i).id.equals(selectedId)) {
                return i;
            }
        }
        return -1;
    }

    private String chineseState(MediaAsset asset) {
        switch (asset.analysisState) {
            case COPYING:
                return "正在准备";
            case LOCAL_READY:
                return "已复制";
            case METADATA_READY:
                return "基础信息完成";
            case KEYFRAMES_READY:
                return "关键帧完成";
            case FAILED:
                return "分析失败";
            case SELECTED:
            default:
                return "已选择";
        }
    }

    private void postStatus(String message) {
        runOnUiThread(() -> setStatus(message));
    }

    private void setStatus(String message) {
        statusView.setTextColor(0xFF146C5A);
        statusView.setText(message);
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusView.setTextColor(0xFFB91C1C);
            statusView.setText(message);
            refreshTimeline();
        });
    }

    private static String previewErrorMessage(Exception error) {
        String detail = error == null ? "" : error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return "播放器无法渲染当前视频。";
        }
        return detail;
    }

    private static final class ThumbnailJob {
        final File mediaFile;
        final TimelineThumbnailRequest request;

        ThumbnailJob(File mediaFile, TimelineThumbnailRequest request) {
            this.mediaFile = mediaFile;
            this.request = request;
        }
    }

    private Button toolButton(String text, View.OnClickListener listener) {
        Button button = compactButton(text);
        button.setOnClickListener(listener);
        return button;
    }

    private Button compactButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(36));
        button.setMinWidth(0);
        button.setPadding(dp(6), 0, dp(6), 0);
        return button;
    }

    private TextView label(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams toolWeight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String formatMs(long value) {
        long totalSeconds = Math.max(0L, value) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.CHINA, "%02d:%02d", minutes, seconds);
    }
}
