package com.litecut.android;

import com.litecut.core.plan.FfmpegCommand;

public interface FfmpegExecutor {
    FfmpegResult execute(FfmpegCommand command);
}
