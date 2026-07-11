package com.litecut.core.plan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class ConcatListWriter {
    public void write(File listFile, List<File> files) throws IOException {
        File parent = listFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create concat list directory: " + parent);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(listFile))) {
            for (File file : files) {
                writer.write("file '");
                writer.write(file.getAbsolutePath().replace("'", "'\\''"));
                writer.write("'");
                writer.newLine();
            }
        }
    }
}
