package com.litecut;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class EditorUiInstrumentedTest {
    @Test
    public void editorShowsChineseControls() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                assertTrue(hasText(root, "导入"));
                assertTrue(hasText(root, "切割"));
                assertTrue(hasText(root, "复制"));
                assertTrue(hasText(root, "删除"));
                assertTrue(hasText(root, "导出"));
                assertTrue(hasText(root, "放大"));
                assertTrue(hasText(root, "缩小"));
                assertTrue(hasText(root, "LiteCut 轻剪辑"));
            });
        }
    }

    @Test
    public void editorContainsTimelineDescription() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                assertTrue(hasContentDescription(root, "时间轴"));
                assertTrue(hasContentDescription(root, "预览"));
                assertTrue(hasContentDescription(root, "放大时间轴"));
                assertTrue(hasContentDescription(root, "缩小时间轴"));
            });
        }
    }

    private static boolean hasText(View view, String text) {
        if (view instanceof TextView) {
            CharSequence actual = ((TextView) view).getText();
            if (actual != null && text.contentEquals(actual)) {
                return true;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasText(group.getChildAt(i), text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasContentDescription(View view, String description) {
        CharSequence actual = view.getContentDescription();
        if (actual != null && description.contentEquals(actual)) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasContentDescription(group.getChildAt(i), description)) {
                    return true;
                }
            }
        }
        return false;
    }
}
