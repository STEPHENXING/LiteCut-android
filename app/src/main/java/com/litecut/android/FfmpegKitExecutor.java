package com.litecut.android;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.FFprobeSession;
import com.arthenica.ffmpegkit.ReturnCode;

import com.litecut.core.plan.FfmpegCommand;

public final class FfmpegKitExecutor implements FfmpegExecutor {
    @Override
    public FfmpegResult execute(FfmpegCommand command) {
        if (command.kind == FfmpegCommand.Kind.FFPROBE) {
            FFprobeSession session = FFprobeKit.executeWithArguments(command.arguments.toArray(new String[0]));
            return result(session.getReturnCode(), session.getOutput(), session.getFailStackTrace());
        }
        FFmpegSession session = FFmpegKit.executeWithArguments(command.arguments.toArray(new String[0]));
        return result(session.getReturnCode(), session.getOutput(), session.getFailStackTrace());
    }

    private static FfmpegResult result(ReturnCode returnCode, String output, String failStackTrace) {
        boolean success = ReturnCode.isSuccess(returnCode);
        int code = returnCode == null ? -1 : returnCode.getValue();
        return new FfmpegResult(success, code, output, failStackTrace);
    }
}
