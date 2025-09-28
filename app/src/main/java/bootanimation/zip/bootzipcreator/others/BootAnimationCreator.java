package bootanimation.zip.bootzipcreator.others;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import bootanimation.zip.bootzipcreator.callbacks.DatabaseWriteCallback;
import bootanimation.zip.bootzipcreator.models.BootAnimation;

public class BootAnimationCreator {

    private static final String TAG = "BootAnimationCreator";

    private final Activity activity;
    private final Uri videoUri;
    private final int videoWidth, videoHeight, fps;
    private final int totalFrames, startFrame, endFrame;
    private final ProgressListener progressListener;
    private final String videoPath;


    // Interface to communicate progress back to the UI
    public interface ProgressListener {
        void onProgress(int progress);
        void onStatusUpdate(String status);
        void onComplete(String zipFilePath);
        void onError(String message);
    }

    public BootAnimationCreator(Activity activity, Uri videoUri, int videoWidth, int videoHeight, int fps, int totalFrames, float startFrame, float endFrame, ProgressListener listener) {
        this.activity = activity;
        this.videoUri = videoUri;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.fps = fps;
        this.totalFrames = totalFrames;
        this.startFrame = (int) startFrame;
        this.endFrame = (int) endFrame;
        this.progressListener = listener;
        this.videoPath = getFilePathFromContentUri(videoUri);
    }

    /**
     * Copies a file from a content URI to the app's cache directory and returns its absolute path.
     * This is necessary because FFmpeg cannot directly access content URIs.
     *
     * @param contentUri The content URI of the file to copy.
     * @return The absolute path to the copied file, or null if an error occurs.
     */
    private String getFilePathFromContentUri(Uri contentUri) {
        String fileName = getFileName(contentUri);
        if (fileName == null) {
            fileName = "temp_video"; // Fallback filename
        }

        File cacheDir = activity.getCacheDir();
        File tempFile = new File(cacheDir, fileName);

        try (InputStream inputStream = activity.getContentResolver().openInputStream(contentUri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[4 * 1024]; // 4K buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy file from content URI", e);
            return null;
        }
    }

    /**
     * Helper method to get the original file name from a content URI.
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if(result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }


    /**
     * Starts the process of creating the boot animation on a background thread.
     */
    public void create() {
        new Thread(() -> {
            try {
                if (videoPath == null) {
                    if (progressListener != null) {
                        activity.runOnUiThread(() -> progressListener.onError("Could not access the selected video file."));
                    }
                    return;
                }
                if (extractFrames()) {
                    compressToZip();
                }
            } catch (Exception e) {
                Log.e(TAG, "Boot animation creation failed", e);
                if (progressListener != null) {
                    activity.runOnUiThread(() -> progressListener.onError(e.getMessage()));
                }
            }
        }).start();
    }

    private boolean extractFrames() {
        if (progressListener != null) {
            activity.runOnUiThread(() -> progressListener.onStatusUpdate("Preparing directories..."));
        }

        String tempDirPath = activity.getExternalFilesDir(null) + "/temporary";
        String part0DirPath = tempDirPath + "/part0";
        String part1DirPath = tempDirPath + "/part1";
        String part2DirPath = tempDirPath + "/part2";
        String descFilePath = tempDirPath + "/desc.txt";

        File tempDir = new File(tempDirPath);
        deleteRecursive(tempDir); // Clean up previous files
        tempDir.mkdirs();
        new File(part0DirPath).mkdirs();
        new File(part1DirPath).mkdirs();
        new File(part2DirPath).mkdirs();
        File descFile = new File(descFilePath);

        try {
            descFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "Error creating desc.txt file", e);
            if (progressListener != null) {
                activity.runOnUiThread(() -> progressListener.onError("Failed to create description file."));
            }
            return false;
        }

        int part0_start_index = 0;
        int part0_end_index = startFrame - 2;

        int part1_start_index = startFrame - 1;
        int part1_end_index = endFrame - 1;

        int part2_start_index = endFrame;
        int part2_end_index = totalFrames - 1;

        long totalExtractionFrames = (part0_end_index >= part0_start_index ? part0_end_index - part0_start_index + 1 : 0) +
                (part1_end_index >= part1_start_index ? part1_end_index - part1_start_index + 1 : 0) +
                (part2_end_index >= part2_start_index ? part2_end_index - part2_start_index + 1 : 0);
        final long[] extractedFrames = {0};

        Config.enableStatisticsCallback(statistics -> {
            extractedFrames[0]++;
            // Frame extraction gets 80% of the total progress. Zipping gets the last 20%.
            int progress = (int) ((extractedFrames[0] * 80.0) / totalExtractionFrames) ;
            if (progressListener != null) {
                activity.runOnUiThread(() -> progressListener.onProgress(progress));
            }
        });

        // Part 0
        if (part0_end_index >= part0_start_index) {
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onStatusUpdate("Extracting part 0..."));
            if (!executeFfmpegCommand(part0_start_index, part0_end_index, part0DirPath)) return false;
        }

        // Part 1
        if (part1_end_index >= part1_start_index) {
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onStatusUpdate("Extracting part 1 (loop)..."));
            if (!executeFfmpegCommand(part1_start_index, part1_end_index, part1DirPath)) return false;
        }

        // Part 2
        if (part2_end_index >= part2_start_index) {
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onStatusUpdate("Extracting part 2..."));
            if (!executeFfmpegCommand(part2_start_index, part2_end_index, part2DirPath)) return false;
        }

        if (progressListener != null) activity.runOnUiThread(() -> progressListener.onStatusUpdate("Creating description file..."));
        try (FileWriter writer = new FileWriter(descFile)) {
            writer.append(String.format(Locale.US, "%d %d %d\n", videoWidth, videoHeight, fps));
            if (Objects.requireNonNull(new File(part0DirPath).listFiles()).length > 0) writer.append("p 1 0 part0\n");
            if (Objects.requireNonNull(new File(part1DirPath).listFiles()).length > 0) writer.append("p 0 0 part1\n");
            if (Objects.requireNonNull(new File(part2DirPath).listFiles()).length > 0) writer.append("p 1 0 part2\n");
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to desc.txt", e);
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onError("Failed to write description file."));
            return false;
        } finally {
            Config.enableStatisticsCallback(null);
        }
        return true;
    }

    private boolean executeFfmpegCommand(int start, int end, String outputPath) {
        // Use %05d to ensure proper file ordering for up to 99999 frames
        String command = String.format(Locale.US, "-i \"%s\" -vf \"select='between(n,%d,%d)',scale=%d:%d\" -vsync 0 \"%s/frame%%05d.png\"",
                videoPath, start, end, videoWidth, videoHeight, outputPath);

        Log.d(TAG, "Executing FFmpeg command: " + command);
        int rc = FFmpeg.execute(command);
        if (rc != Config.RETURN_CODE_SUCCESS) {
            Log.e(TAG, "FFmpeg command failed with rc=" + rc);
            Config.printLastCommandOutput(Log.ERROR);
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onError("Frame extraction failed."));
            return false;
        }
        return true;
    }

    private void compressToZip() {
        if (progressListener != null) {
            activity.runOnUiThread(() -> {
                progressListener.onStatusUpdate("Compressing files...");
                progressListener.onProgress(85); // Start zipping progress
            });
        }
        String sourceDirPath = activity.getExternalFilesDir(null) + "/temporary";
        File outputDir = new File(activity.getExternalFilesDir(null), "Boot Animation Files");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String timestamp = CustomMethods.getCurrentTimestamp();
        String zipFileName = "bootanimation_" + timestamp + ".zip";
        String zipFilePath = new File(outputDir, zipFileName).getAbsolutePath();

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File sourceDir = new File(sourceDirPath);
            // We pass an empty string for the parent folder so that the contents (part0, desc.txt) are at the root of the zip.
            zipFile(sourceDir, "", zos);
            activity.runOnUiThread(() -> {
                if (progressListener != null) {
                    progressListener.onProgress(95);
                }
            });


        } catch (IOException e) {
            Log.e(TAG, "Error creating zip file", e);
            if (progressListener != null) activity.runOnUiThread(() -> progressListener.onError("Failed to create zip file."));
            return;
        }

        // Save video and record to DB
        String savedVideoPath = saveVideoPermanently();
        if (savedVideoPath != null) {

            BootAnimation bootAnimation = new BootAnimation(
                    savedVideoPath,
                    zipFilePath,
                    new File(savedVideoPath).getName(),
                    zipFileName,
                    System.currentTimeMillis()
            );

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(activity);

            dbHelper.addBootAnimationAsync(bootAnimation, new DatabaseWriteCallback() {
                @Override
                public void onSuccess() {
                    // Runs on UI thread ✅
                    Toast.makeText(activity, "Stored in database successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    // Runs on UI thread ✅
                    Log.e("DB_ERROR", "Could not save animation", e);
                    Toast.makeText(activity, "Error saving animation into database.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Cleanup
        deleteRecursive(new File(sourceDirPath));
        deleteRecursive(new File(videoPath)); // Also delete the cached video file

        if (progressListener != null) {
            activity.runOnUiThread(() -> {
                progressListener.onProgress(100);
                progressListener.onComplete(zipFilePath);
            });
        }
    }

    private String saveVideoPermanently() {
        if (videoPath == null) return null;

        File videoStorageDir = new File(activity.getExternalFilesDir(null), "Animation Videos");
        if (!videoStorageDir.exists()) {
            videoStorageDir.mkdirs();
        }

        String timestamp = CustomMethods.getCurrentTimestamp();
        String originalFileName = getFileName(videoUri);
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newVideoFileName = "video_" + timestamp + fileExtension;
        File newVideoFile = new File(videoStorageDir, newVideoFileName);

        try (InputStream in = new FileInputStream(videoPath);
             OutputStream out = new FileOutputStream(newVideoFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return newVideoFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save video permanently", e);
            return null;
        }
    }


    private void zipFile(File fileToZip, String parentPath, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            String entryPath = parentPath.isEmpty() ? "" : parentPath + "/";
            File[] children = fileToZip.listFiles();
            if(children == null) return;
            for (File childFile : children) {
                zipFile(childFile, entryPath + childFile.getName(), zos);
            }
            return;
        }

        // First compute CRC32 and size for STORED mode, which is required for bootanimation.zip
        CRC32 crc = new CRC32();
        long size = 0;
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                crc.update(buffer, 0, len);
                size += len;
            }
        }

        // Now create entry with STORED method
        ZipEntry zipEntry = new ZipEntry(parentPath);
        zipEntry.setMethod(ZipEntry.STORED);
        zipEntry.setSize(size);
        zipEntry.setCompressedSize(size); // must equal size for STORED
        zipEntry.setCrc(crc.getValue());
        zos.putNextEntry(zipEntry);

        // Write file contents again
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
        }
        zos.closeEntry();
    }


    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}

