package com.ichi2.anki;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.preferences.PreferenceExtensions;
import com.ichi2.utils.FileUtil;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class StorageMigrator {

    public static final int CREATE_REQUEST_CODE = 40;
    public static final int OPEN_REQUEST_CODE = 41;
    public static final int SAVE_REQUEST_CODE = 42;

    public static void testDirectories(Context context) {
        Timber.i("DIR LEGACY: %s", CollectionHelper.getLegacyAnkiDroidDirectory());
        Timber.i("DIR SCOPED INTERNAL: %s", CollectionHelper.getSystemDefaultInternalAnkiDroidDirectory(context));
        Timber.i("DIR SCOPED EXTERNAL: %s", CollectionHelper.getSystemDefaultExternalAnkiDroidDirectory(context));
    }

    /**
     * Checks if current directory stored in SharedPreferences is a Legacy Storage Directory.
     * @param context Context used to access SharedPreferences.
     * @return returns true if App is using Legacy Storage.
     */
    public static boolean isLegacyStorage(Context context) {
        String legacyDir = CollectionHelper.getLegacyAnkiDroidDirectory();

        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);

        // Returns current directory
        String currentDir = PreferenceExtensions.getOrSetString(
                preferences,
                "deckPath",
                CollectionHelper::getLegacyAnkiDroidDirectory);

        Timber.i("IS LEGACY STORAGE: %s %s", currentDir, legacyDir);
        return currentDir.equals(legacyDir);
    }

    /**
     * Migrates user data from Legacy Storage Directory to Scoped Storage Directory
     * @param activity Activity context
     * @return Returns true if migration was successful
     */
    public static boolean migrateToScoped(Activity activity) {
        String sourceDirectory = CollectionHelper.getLegacyAnkiDroidDirectory();
        String destinationDirectory = CollectionHelper.getSystemDefaultExternalAnkiDroidDirectory(activity);

        Timber.i("STARTING MIGRATION %s to %s", sourceDirectory, destinationDirectory);
        boolean migrationComplete = FileUtil.copyDirectory(sourceDirectory, destinationDirectory);
        Timber.i("COMPLETED MIGRATION %b", migrationComplete);

        if (migrationComplete) {
            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(activity);
            PreferenceExtensions.setString(preferences, "deckPath", destinationDirectory);
        }

        return migrationComplete;
    }
}
