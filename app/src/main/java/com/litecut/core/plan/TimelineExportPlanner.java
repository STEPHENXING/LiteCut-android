package com.litecut.core.plan;

import com.litecut.core.model.EditRange;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.timeline.MediaAsset;
import com.litecut.core.timeline.TimelineClip;
import com.litecut.core.timeline.TimelineProject;
import com.litecut.core.validation.MergeValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TimelineExportPlanner {
    private final GopRangeExpander rangeExpander = new GopRangeExpander();
    private final MergeValidator mergeValidator = new MergeValidator();

    public List<FfmpegCommand> planTimelineExport(TimelineProject project, File tempDir, File output)
            throws PlanException {
        if (project.clips().isEmpty()) {
            throw new PlanException("时间轴中没有可导出的片段。");
        }
        if (tempDir == null || output == null) {
            throw new PlanException("导出目录无效。");
        }

        ArrayList<MediaMetadata> clipMetadata = new ArrayList<>();
        for (TimelineClip clip : project.clips()) {
            MediaAsset asset = project.assetForClip(clip);
            requireExportReady(asset);
            clipMetadata.add(asset.metadata);
        }
        if (clipMetadata.size() > 1) {
            mergeValidator.validate(clipMetadata);
        }

        ArrayList<FfmpegCommand> commands = new ArrayList<>();
        ArrayList<File> segmentFiles = new ArrayList<>();
        for (int i = 0; i < project.clips().size(); i++) {
            TimelineClip clip = project.clips().get(i);
            MediaAsset asset = project.assetForClip(clip);
            EditRange range = rangeExpander.expand(
                    clip.sourceInMs,
                    clip.sourceOutMs,
                    asset.metadata.durationMs,
                    asset.metadata.keyframeMs
            );
            File segment = new File(tempDir, String.format(java.util.Locale.US, "timeline_%03d.mp4", i));
            segmentFiles.add(segment);
            commands.add(segmentCommand(asset.metadata, asset.localWorkspaceFile, segment, range));
        }

        if (segmentFiles.size() == 1) {
            commands.add(copySegmentCommand(segmentFiles.get(0), output));
        } else {
            File concatList = new File(tempDir, "timeline_concat.txt");
            commands.add(concatCommand(concatList, output, segmentFiles));
        }
        return commands;
    }

    public List<EditRange> effectiveRanges(TimelineProject project) throws PlanException {
        ArrayList<EditRange> ranges = new ArrayList<>();
        for (TimelineClip clip : project.clips()) {
            MediaAsset asset = project.assetForClip(clip);
            requireKeyframes(asset);
            ranges.add(rangeExpander.expand(clip.sourceInMs, clip.sourceOutMs, asset.metadata.durationMs, asset.metadata.keyframeMs));
        }
        return ranges;
    }

    private static void requireExportReady(MediaAsset asset) throws PlanException {
        if (asset == null) {
            throw new PlanException("素材不存在。");
        }
        if (!asset.hasLocalFile()) {
            throw new PlanException("素材尚未准备好导出文件：" + asset.displayName);
        }
        requireKeyframes(asset);
    }

    private static void requireKeyframes(MediaAsset asset) throws PlanException {
        if (!asset.hasMetadata()) {
            throw new PlanException("素材尚未完成基础分析：" + asset.displayName);
        }
        if (!asset.hasKeyframes()) {
            throw new PlanException("素材关键帧仍在分析中：" + asset.displayName);
        }
    }

    private FfmpegCommand segmentCommand(MediaMetadata metadata, File input, File output, EditRange range) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-ss", seconds(range.effectiveInMs),
                "-i", input.getAbsolutePath(),
                "-t", seconds(range.effectiveDurationMs()),
                "-map", "0:v:0"
        ));
        if (metadata.hasAudio()) {
            args.add("-map");
            args.add("0:a?");
        }
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, Collections.singletonList(output), Collections.emptyList());
    }

    private FfmpegCommand copySegmentCommand(File input, File output) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-i", input.getAbsolutePath()
        ));
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, Collections.singletonList(input), Collections.emptyList());
    }

    private FfmpegCommand concatCommand(File concatList, File output, List<File> inputs) {
        ArrayList<File> temporaryFiles = new ArrayList<>(inputs);
        temporaryFiles.add(concatList);
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.getAbsolutePath()
        ));
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, temporaryFiles, inputs);
    }

    private static void addCopyOutputFlags(ArrayList<String> args) {
        args.add("-c");
        args.add("copy");
        args.add("-avoid_negative_ts");
        args.add("make_zero");
    }

    private static FfmpegCommand command(List<String> args, File output, List<File> temporaryFiles, List<File> concatInputs) {
        ensureLossless(args);
        return new FfmpegCommand(FfmpegCommand.Kind.FFMPEG, args, output, temporaryFiles, concatInputs);
    }

    private static void ensureLossless(List<String> args) {
        String joined = args.toString();
        if (joined.contains("-vf") || joined.contains("-filter") || joined.contains("scale=")) {
            throw new IllegalArgumentException("Timeline export plans must not use filters or scaling.");
        }
    }

    private static String seconds(long milliseconds) {
        return String.format(java.util.Locale.US, "%.3f", milliseconds / 1000.0);
    }
}
