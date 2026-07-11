package com.litecut.android;

import com.litecut.core.plan.ConcatListWriter;
import com.litecut.core.plan.FfmpegCommand;
import com.litecut.core.progress.SizeProgressEstimator;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class OperationRunner {
    private final FfmpegExecutor executor;
    private final ConcatListWriter concatListWriter = new ConcatListWriter();
    private final SizeProgressEstimator progressEstimator = new SizeProgressEstimator();

    public OperationRunner(FfmpegExecutor executor) {
        this.executor = executor;
    }

    public FfmpegResult run(List<FfmpegCommand> commands, long expectedBytes, ProgressCallback callback) {
        FfmpegResult last = null;
        for (FfmpegCommand command : commands) {
            try {
                prepareConcatList(command);
            } catch (IOException exception) {
                return new FfmpegResult(false, -1, "", exception.getMessage());
            }
            if (callback != null) {
                callback.onProgress(progressEstimator.percent(command.outputFile, expectedBytes));
            }
            last = executor.execute(command);
            if (!last.success) {
                return last;
            }
        }
        if (callback != null) {
            callback.onProgress(100);
        }
        return last == null ? new FfmpegResult(false, -1, "", "No commands to run.") : last;
    }

    public void cleanupTemporaryFiles(List<FfmpegCommand> commands) {
        for (FfmpegCommand command : commands) {
            for (File file : command.temporaryFiles) {
                if (file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }

    private void prepareConcatList(FfmpegCommand command) throws IOException {
        if (command.concatInputFiles.isEmpty()) {
            return;
        }
        File listFile = null;
        for (int i = 0; i < command.arguments.size() - 1; i++) {
            if ("-i".equals(command.arguments.get(i))) {
                listFile = new File(command.arguments.get(i + 1));
            }
        }
        if (listFile != null) {
            concatListWriter.write(listFile, command.concatInputFiles);
        }
    }

    public interface ProgressCallback {
        void onProgress(int percent);
    }
}
