package com.litecut.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class AndroidMediaAccess {
    private static final int BUFFER_SIZE = 1024 * 1024;

    public Intent videoOpenIntent(boolean allowMultiple) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    public void persistReadPermission(Context context, Uri uri, int resultFlags) {
        int flags = resultFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        if (flags == 0) {
            return;
        }
        try {
            context.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // Photo Picker URIs and some providers are intentionally non-persistable.
        }
    }

    public File copyToWorkspace(Context context, Uri uri, String fallbackName) throws IOException {
        File workspace = workspaceDir(context);
        if (!workspace.exists() && !workspace.mkdirs()) {
            throw new IOException("Cannot create workspace: " + workspace);
        }
        File target = uniqueFile(workspace, safeName(fallbackName, "input.mp4"));
        try (
                InputStream input = context.getContentResolver().openInputStream(uri);
                OutputStream output = new FileOutputStream(target)
        ) {
            if (input == null) {
                throw new IOException("Cannot open selected media.");
            }
            copy(input, output);
        }
        return target;
    }

    public String displayName(Context context, Uri uri, String fallbackName) {
        try (android.database.Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) {
                        return safeName(value, fallbackName);
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return safeName(fallbackName, "input.mp4");
    }

    public Uri publishVideo(Context context, File source, String displayName) throws IOException {
        if (isAudioFile(source)) {
            return publishAudio(context, source, displayName);
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName(displayName, source.getName()));
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(source, "video/mp4"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LiteCut");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Cannot create MediaStore output.");
        }
        try (InputStream input = new java.io.FileInputStream(source);
             OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Cannot open MediaStore output.");
            }
            copy(input, output);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        }
        return uri;
    }

    private Uri publishAudio(Context context, File source, String displayName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName(displayName, source.getName()));
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(source, "audio/mp4"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/LiteCut");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Cannot create MediaStore audio output.");
        }
        try (InputStream input = new java.io.FileInputStream(source);
             OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Cannot open MediaStore audio output.");
            }
            copy(input, output);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        }
        return uri;
    }

    public File workspaceDir(Context context) {
        return new File(context.getFilesDir(), "media-workspace");
    }

    public File tempDir(Context context) {
        return new File(context.getCacheDir(), "litecut-temp");
    }

    public void cleanupTemp(Context context) {
        deleteChildren(tempDir(context));
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    private static File uniqueFile(File directory, String name) {
        File candidate = new File(directory, name);
        int index = 1;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        while (candidate.exists()) {
            candidate = new File(directory, base + "_" + index + ext);
            index++;
        }
        return candidate;
    }

    private static String safeName(String preferred, String fallback) {
        String value = preferred == null || preferred.trim().isEmpty() ? fallback : preferred;
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String mimeFor(File file, String fallback) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return fallback;
        }
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1));
        return mime == null ? fallback : mime;
    }

    private static boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase(java.util.Locale.US);
        return name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".mp3") || name.endsWith(".wav");
    }

    private static void deleteChildren(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteChildren(file);
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
