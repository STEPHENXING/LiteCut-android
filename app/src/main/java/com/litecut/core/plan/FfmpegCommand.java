package com.litecut.core.plan;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FfmpegCommand {
    public enum Kind {
        FFMPEG,
        FFPROBE
    }

    public final Kind kind;
    public final List<String> arguments;
    public final File outputFile;
    public final List<File> temporaryFiles;
    public final List<File> concatInputFiles;

    public FfmpegCommand(Kind kind, List<String> arguments, File outputFile, List<File> temporaryFiles) {
        this(kind, arguments, outputFile, temporaryFiles, Collections.emptyList());
    }

    public FfmpegCommand(
            Kind kind,
            List<String> arguments,
            File outputFile,
            List<File> temporaryFiles,
            List<File> concatInputFiles
    ) {
        this.kind = kind;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        this.outputFile = outputFile;
        this.temporaryFiles = Collections.unmodifiableList(new ArrayList<>(temporaryFiles));
        this.concatInputFiles = Collections.unmodifiableList(new ArrayList<>(concatInputFiles));
    }

    public String toShellLikeString() {
        StringBuilder builder = new StringBuilder(kind == Kind.FFPROBE ? "ffprobe" : "ffmpeg");
        for (String argument : arguments) {
            builder.append(' ').append(quote(argument));
        }
        return builder.toString();
    }

    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        if (!value.contains(" ") && !value.contains("'") && !value.contains("\"")) {
            return value;
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
}
