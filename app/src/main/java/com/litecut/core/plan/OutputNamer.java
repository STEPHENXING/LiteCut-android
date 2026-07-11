package com.litecut.core.plan;

import java.io.File;

public final class OutputNamer {
    public File defaultOutput(File source, File directory, String suffix, String extensionOverride) {
        String sourceName = source == null ? "litecut" : source.getName();
        int dot = sourceName.lastIndexOf('.');
        String base = dot > 0 ? sourceName.substring(0, dot) : sourceName;
        String extension = extensionOverride != null && !extensionOverride.isEmpty()
                ? extensionOverride
                : (dot > 0 ? sourceName.substring(dot) : ".mp4");
        File parent = directory != null ? directory : (source == null ? new File(".") : source.getParentFile());
        File candidate = new File(parent, base + suffix + extension);
        int index = 1;
        while (candidate.exists()) {
            candidate = new File(parent, base + suffix + "_" + index + extension);
            index++;
        }
        return candidate;
    }
}
