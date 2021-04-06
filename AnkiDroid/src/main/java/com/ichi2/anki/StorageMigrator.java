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
     * Requests storage permission for a directory by launching the device's System File Picker.
     * @param context Activity context to launch Intent.
     */
    public static void requestLegacyStoragePermission(Activity context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        context.startActivityForResult(intent, OPEN_REQUEST_CODE);
    }


    /**
     * Persists Uri with permission grant provided by user via System Picker so that it is persisted beyond device reboots.
     * @param context Context required for Content Resolver
     * @param intent Intent returned onActivityResult() after a user has responded to a permission request sent via System File Picker.
     * @return returns true if the intent had a valid uri with a permission grant that has now been persisted.
     */
    public static boolean persistUriPermission(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return false;
        }

        // Persist URI Permission grant
        final int takeFlags = intent.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Timber.i("persistUriPermission() called %d", takeFlags);
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        return true;
    }


    /**
     * Migrates user data from Legacy Storage Directory to Scoped Storage Directory. Accesses Legacy Storage using Storage Access Framework
     * @param activity Activity context
     * @param uri Directory root's Uri which contains permission granted via System File Picker
     * @return Returns true if migration was successful
     */
    public static boolean migrateToScoped(Activity activity, Uri uri) {
        String sourceDirectory = CollectionHelper.getLegacyAnkiDroidDirectory();
        String destinationDirectory = CollectionHelper.getSystemDefaultExternalAnkiDroidDirectory(activity);

        Timber.i("STARTING MIGRATION %s to %s", sourceDirectory, destinationDirectory);
        boolean migrationComplete = FileUtil.copyDirectory(activity, uri, destinationDirectory);
        Timber.i("COMPLETED MIGRATION %b", migrationComplete);

        if (migrationComplete) {
            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(activity);
            PreferenceExtensions.setString(preferences, "deckPath", destinationDirectory);
        }

        return migrationComplete;
    }
}
