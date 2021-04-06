package com.ichi2.anki;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.ichi2.preferences.PreferenceExtensions;
import com.ichi2.utils.FileUtil;

import java.io.File;

import timber.log.Timber;

public class StorageMigrator {

    private static StorageMigrator instance = null;

    public static final int CREATE_REQUEST_CODE = 40;
    public static final int OPEN_REQUEST_CODE = 41;
    public static final int SAVE_REQUEST_CODE = 42;

    public static final int STORAGE_SELECT_LEGACY = 300;
    public static final int STORAGE_SELECT_SCOPED = 301;

    public int storageOption;

    private StorageMigrator() {
        storageOption = STORAGE_SELECT_LEGACY;
    }

    public synchronized static StorageMigrator getInstance() {
        if (instance == null) {
            instance = new StorageMigrator();
        }
        return instance;
    }

    @SuppressWarnings("deprecation")
    public void testDirectories(Context context) {
        Timber.i("DIR LEGACY: %s", getLegacyAnkiDroidDirectory());
        Timber.i("DIR SCOPED INTERNAL: %s", getSystemDefaultInternalAnkiDroidDirectory(context));
        Timber.i("DIR SCOPED EXTERNAL: %s", getSystemDefaultExternalAnkiDroidDirectory(context));

        // Directory for Whiteboard pictures
        Timber.i("DIR SCOPED EXTERNAL PICTURES: %s", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
    }

    public boolean isLegacyStorage(Context context) {
        String legacyDir = getLegacyAnkiDroidDirectory();
        MediaStore.Images
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);

        // Returns current directory
        String currentDir = PreferenceExtensions.getOrSetString(
                preferences,
                "deckPath",
                legacyDir);

        Timber.i("IS LEGACY STORAGE: %s %s", currentDir, legacyDir);

        return currentDir.equals(legacyDir);
    }

    public void requestLegacyStoragePermission(Activity context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        context.startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public boolean persistUriPermission(Context context, Intent intent) {
        Uri uri = intent.getData();

        if (uri == null) {
            return false;
        }

        // Persist URI Permission grant
        final int takeFlags = intent.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Timber.i("persistUriPermission() called %d", takeFlags);
        // Check for the freshest data.
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);

        return true;
    }

    public boolean migrateToScoped(Activity activity, Uri uri, boolean usingScopedStorage) {
        String sourceDirectory = getLegacyAnkiDroidDirectory();
        String destinationDirectory = getSystemDefaultExternalAnkiDroidDirectory(activity);

        Timber.i("STARTING MIGRATION %s to %s", sourceDirectory, destinationDirectory);
        boolean migrationComplete = FileUtil.moveFile(activity, uri, usingScopedStorage, sourceDirectory, destinationDirectory);
        Timber.i("COMPLETED MIGRATION %b", migrationComplete);

        if (migrationComplete) {
            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(activity);
            PreferenceExtensions.setString(preferences, "deckPath", destinationDirectory);
        }

        return migrationComplete;
    }

    public String getCurrentAnkiDroidDirectory(Context context) {
        String targetDir = getLegacyAnkiDroidDirectory();

        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);

        // Returns current directory
        return PreferenceExtensions.getOrSetString(
                preferences,
                "deckPath",
                targetDir);
    }

    @SuppressWarnings("deprecation")
    public String getLegacyAnkiDroidDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "AnkiDroid").getAbsolutePath();
    }

    public String getSystemDefaultExternalAnkiDroidDirectory(Context context) {
        return new File(context.getExternalFilesDir(null), "AnkiDroid").getAbsolutePath();
    }

    public String getSystemDefaultInternalAnkiDroidDirectory(Context context) {
        return new File(context.getFilesDir(), "AnkiDroid").getAbsolutePath();
    }
}
