package com.litecut.android;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class AndroidMediaAccessInstrumentedTest {
    @Test
    public void videoOpenIntentRequestsPersistableReadAccess() {
        Intent intent = new AndroidMediaAccess().videoOpenIntent(true);

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.getAction());
        assertEquals("video/*", intent.getType());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
        assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
    }

    @Test
    public void workspaceAndTempDirectoriesAreAppPrivate() {
        Context context = ApplicationProvider.getApplicationContext();
        AndroidMediaAccess access = new AndroidMediaAccess();

        assertTrue(access.workspaceDir(context).getAbsolutePath().contains(context.getFilesDir().getAbsolutePath()));
        assertTrue(access.tempDir(context).getAbsolutePath().contains(context.getCacheDir().getAbsolutePath()));
    }
}
