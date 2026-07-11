package com.litecut.core.plan;

import com.litecut.core.model.EditRange;
import com.litecut.core.model.MediaMetadata;
import com.litecut.core.validation.MergeValidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OperationPlanner {
    private final GopRangeExpander rangeExpander = new GopRangeExpander();
    private final MergeValidator mergeValidator = new MergeValidator();

    public FfmpegCommand planCut(MediaMetadata metadata, File input, File output, long inMs, long outMs)
            throws PlanException {
        requireUsableVideo(metadata);
        EditRange range = rangeExpander.expand(inMs, outMs, metadata.durationMs, metadata.keyframeMs);
        ArrayList<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(
                "-y",
                "-ss", seconds(range.effectiveInMs),
                "-i", input.getAbsolutePath(),
                "-t", seconds(range.effectiveDurationMs()),
                "-map", "0:v:0"
        ));
        addAudioCopyIfPresent(args, metadata);
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, Collections.emptyList());
    }

    public List<FfmpegCommand> planDelete(MediaMetadata metadata, File input, File tempDir, File output, long inMs, long outMs)
            throws PlanException {
        requireUsableVideo(metadata);
        EditRange range = rangeExpander.expand(inMs, outMs, metadata.durationMs, metadata.keyframeMs);
        File before = new File(tempDir, "delete_before.mp4");
        File after = new File(tempDir, "delete_after.mp4");
        File concatList = new File(tempDir, "delete_concat.txt");
        ArrayList<FfmpegCommand> commands = new ArrayList<>();
        ArrayList<File> concatFiles = new ArrayList<>();
        if (range.effectiveInMs > 0L) {
            commands.add(segmentCommand(metadata, input, before, 0L, range.effectiveInMs));
            concatFiles.add(before);
        }
        if (range.effectiveOutMs < metadata.durationMs) {
            commands.add(segmentCommand(metadata, input, after, range.effectiveOutMs, metadata.durationMs));
            concatFiles.add(after);
        }
        if (concatFiles.isEmpty()) {
            throw new PlanException("Delete range would remove the entire video.");
        }
        commands.add(concatCommand(concatList, output, concatFiles));
        return commands;
    }

    public List<FfmpegCommand> planRepeat(
            MediaMetadata metadata,
            File input,
            File tempDir,
            File output,
            long inMs,
            long outMs,
            int additionalCopies
    ) throws PlanException {
        requireUsableVideo(metadata);
        if (additionalCopies < 1) {
            throw new PlanException("Repeat count must be at least one additional copy.");
        }
        EditRange range = rangeExpander.expand(inMs, outMs, metadata.durationMs, metadata.keyframeMs);
        File before = new File(tempDir, "repeat_before.mp4");
        File selected = new File(tempDir, "repeat_selected.mp4");
        File after = new File(tempDir, "repeat_after.mp4");
        File concatList = new File(tempDir, "repeat_concat.txt");

        ArrayList<FfmpegCommand> commands = new ArrayList<>();
        if (range.effectiveInMs > 0L) {
            commands.add(segmentCommand(metadata, input, before, 0L, range.effectiveInMs));
        }
        commands.add(segmentCommand(metadata, input, selected, range.effectiveInMs, range.effectiveOutMs));
        if (range.effectiveOutMs < metadata.durationMs) {
            commands.add(segmentCommand(metadata, input, after, range.effectiveOutMs, metadata.durationMs));
        }

        ArrayList<File> concatFiles = new ArrayList<>();
        if (range.effectiveInMs > 0L) {
            concatFiles.add(before);
        }
        concatFiles.add(selected);
        for (int i = 0; i < additionalCopies; i++) {
            concatFiles.add(selected);
        }
        if (range.effectiveOutMs < metadata.durationMs) {
            concatFiles.add(after);
        }
        commands.add(concatCommand(concatList, output, concatFiles));
        return commands;
    }

    public FfmpegCommand planMerge(List<MediaMetadata> metadataList, List<File> inputFiles, File concatList, File output)
            throws PlanException {
        mergeValidator.validate(metadataList);
        if (inputFiles == null || inputFiles.size() != metadataList.size()) {
            throw new PlanException("Merge input files must match probed metadata.");
        }
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.getAbsolutePath()
        ));
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, Collections.singletonList(concatList), inputFiles);
    }

    public FfmpegCommand planExtractAudio(MediaMetadata metadata, File input, File output) throws PlanException {
        if (metadata == null || !metadata.hasAudio()) {
            throw new PlanException("This video has no audio stream.");
        }
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-i", input.getAbsolutePath(),
                "-vn",
                "-map", "0:a:0",
                "-c:a", "copy",
                "-avoid_negative_ts", "make_zero",
                output.getAbsolutePath()
        ));
        return command(args, output, Collections.emptyList());
    }

    public EditRange expandRange(MediaMetadata metadata, long inMs, long outMs) throws PlanException {
        requireUsableVideo(metadata);
        return rangeExpander.expand(inMs, outMs, metadata.durationMs, metadata.keyframeMs);
    }

    private FfmpegCommand segmentCommand(MediaMetadata metadata, File input, File output, long fromMs, long toMs) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(
                "-y",
                "-ss", seconds(fromMs),
                "-i", input.getAbsolutePath(),
                "-t", seconds(toMs - fromMs),
                "-map", "0:v:0"
        ));
        addAudioCopyIfPresent(args, metadata);
        addCopyOutputFlags(args);
        args.add(output.getAbsolutePath());
        return command(args, output, Collections.singletonList(output));
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

    private static void requireUsableVideo(MediaMetadata metadata) throws PlanException {
        if (metadata == null || !metadata.hasVideo()) {
            throw new PlanException("A readable video stream is required.");
        }
        if (metadata.durationMs <= 0L) {
            throw new PlanException("Video duration is required.");
        }
    }

    private static void addAudioCopyIfPresent(ArrayList<String> args, MediaMetadata metadata) {
        if (metadata.hasAudio()) {
            args.add("-map");
            args.add("0:a?");
        }
    }

    private static void addCopyOutputFlags(ArrayList<String> args) {
        args.add("-c");
        args.add("copy");
        args.add("-avoid_negative_ts");
        args.add("make_zero");
    }

    private static FfmpegCommand command(List<String> args, File output, List<File> temporaryFiles) {
        return command(args, output, temporaryFiles, Collections.emptyList());
    }

    private static FfmpegCommand command(List<String> args, File output, List<File> temporaryFiles, List<File> concatInputs) {
        ensureLossless(args);
        return new FfmpegCommand(FfmpegCommand.Kind.FFMPEG, args, output, temporaryFiles, concatInputs);
    }

    private static void ensureLossless(List<String> args) {
        String joined = args.toString();
        if (joined.contains("-vf") || joined.contains("-filter") || joined.contains("scale=")) {
            throw new IllegalArgumentException("LiteCut plans must not use filters or scaling.");
        }
    }

    private static String seconds(long milliseconds) {
        return String.format(java.util.Locale.US, "%.3f", milliseconds / 1000.0);
    }
}
