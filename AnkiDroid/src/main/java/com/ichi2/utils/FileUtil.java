package com.ichi2.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.StatFs;
import android.provider.DocumentsContract;

import com.ichi2.compat.CompatHelper;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;

public class FileUtil {
    /** Gets the free disk space given a file */
    public static long getFreeDiskSpace(File file, long defaultValue) {
        try {
            return new StatFs(file.getParentFile().getPath()).getAvailableBytes();
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Free space could not be retrieved");
            return defaultValue;
        }
    }

    /**
     *
     * @param uri               uri to the content to be internalized, used if filePath not real/doesn't work.
     * @param filePath          path to the file to be internalized.
     * @param internalFile      an internal cache temp file that the data is copied/internalized into.
     * @param contentResolver   this is needed to open an inputStream on the content uri.
     * @return  the internal file after copying the data across.
     * @throws IOException
     */
    @NonNull
    public static File internalizeUri(
            Uri uri, @Nullable String filePath, File internalFile, ContentResolver contentResolver
    ) throws IOException {

        // If we got a real file name, do a copy from it
        InputStream inputStream;
        if (filePath != null) {
            Timber.d("internalizeUri() got file path for direct copy from Uri %s", uri);
            try {
                inputStream = new FileInputStream(new File(filePath));
            } catch (FileNotFoundException e) {
                Timber.w(e, "internalizeUri() unable to open input stream on file %s", filePath);
                throw e;
            }
        } else {
            try {
                inputStream = contentResolver.openInputStream(uri);
            } catch (Exception e) {
                Timber.w(e, "internalizeUri() unable to open input stream from content resolver for Uri %s", uri);
                throw e;
            }
        }

        try {
            CompatHelper.getCompat().copyFile(inputStream, internalFile.getAbsolutePath());
        } catch (Exception e) {
            Timber.w(e, "internalizeUri() unable to internalize file from Uri %s to File %s", uri, internalFile.getAbsolutePath());
            throw e;
        }
        return internalFile;
    }


    /**
     * @return Key: Filename; Value: extension including dot
     */
    @Nullable
    public static Map.Entry<String, String> getFileNameAndExtension(@Nullable String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index < 1) {
            return null;
        }

        return new AbstractMap.SimpleEntry<>(fileName.substring(0, index), fileName.substring(index));
    }


    /**
     * Uses Java I/O to move transfer data
     * @param in stream of bytes representing input
     * @param out stream of bytes representing output
     * @return Returns true if data transfered
     */
    public static boolean moveFile(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[1024];

            int length;
            while ((length = in.read(buffer)) > 0)
            {
                out.write(buffer, 0, length);
            }
        } catch (Exception e) {
            try {
                in.close();
            }
            catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            try {
                out.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Copies folder given that the App has access to the Legacy Storage Directory via WRITE_EXTERNAL_STORAGE
     * @param source File representation of source directory
     * @param destination File representation of destination directory
     * @return Returns true if folder copied
     */
    private static boolean copyFolderUnderLegacyStorage(File source, File destination)
    {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] files = source.list();

            if (files == null) {
                return true;
            }
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                Timber.i("copyFolder(%s, %s)", srcFile.getAbsolutePath(),
                        destFile.getAbsolutePath());

                copyFolderUnderLegacyStorage(srcFile, destFile);
            }
        } else {
            try {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(destination);
                moveFile(in, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    /**
     * Moves file represented by sourceUri to output path
     * @param sourceUri Represents source file
     * @param usingScopedStorage Decides appropriate transfer mechanism based on Storage method being used
     * @param inputPath Source path
     * @param outputPath Destination path - will be created if it doesn't exist
     * @return Returns true if successful
     */
    public static boolean moveDirectory(tring inputPath, String outputPath) {

        File sourceDirectory = new File(inputPath);
        File destinationDirectory = new File(outputPath);

        boolean folderCopied = copyFolderUnderLegacyStorage(sourceDirectory, destinationDirectory);
        sourceDirectory.delete();

        return folderCopied;
    }

    /**
     * Copies file represented by sourceUri to output path
     * @param sourceUri Represents source file
     * @param usingScopedStorage Decides appropriate copy mechanism based on Storage method being used
     * @param inputPath Source path
     * @param outputPath Destination path - will be created if it doesn't exist
     * @return Returns true if successful
     */
    public static boolean copyDirectory(String inputPath, String outputPath) {

        File sourceDirectory = new File(inputPath);
        File destinationDirectory = new File(outputPath);
        return copyFolderUnderLegacyStorage(sourceDirectory, destinationDirectory);
    }
}
