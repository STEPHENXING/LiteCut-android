package com.litecut.core.model;

public final class EditRange {
    public final long selectedInMs;
    public final long selectedOutMs;
    public final long effectiveInMs;
    public final long effectiveOutMs;

    public EditRange(long selectedInMs, long selectedOutMs, long effectiveInMs, long effectiveOutMs) {
        this.selectedInMs = selectedInMs;
        this.selectedOutMs = selectedOutMs;
        this.effectiveInMs = effectiveInMs;
        this.effectiveOutMs = effectiveOutMs;
    }

    public long effectiveDurationMs() {
        return Math.max(0L, effectiveOutMs - effectiveInMs);
    }
}
