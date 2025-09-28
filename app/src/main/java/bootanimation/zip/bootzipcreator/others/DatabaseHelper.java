package bootanimation.zip.bootzipcreator.others;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bootanimation.zip.bootzipcreator.callbacks.BootAnimationCallback;
import bootanimation.zip.bootzipcreator.callbacks.DatabaseWriteCallback;
import bootanimation.zip.bootzipcreator.models.BootAnimation;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "boot_animations.db";
    private static final int DATABASE_VERSION = 1;

    // Table and Column constants remain the same...
    public static final String TABLE_BOOT_ANIMATIONS = "boot_animations";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_VIDEO_PATH = "video_path";
    public static final String COLUMN_ZIP_PATH = "zip_path";
    public static final String COLUMN_VIDEO_FILE_NAME = "video_file_name";
    public static final String COLUMN_ZIP_FILE_NAME = "zip_file_name";
    public static final String COLUMN_CREATION_TIME = "creation_time";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_BOOT_ANIMATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_VIDEO_PATH + " TEXT, " +
                    COLUMN_ZIP_PATH + " TEXT, " +
                    COLUMN_VIDEO_FILE_NAME + " TEXT, " +
                    COLUMN_ZIP_FILE_NAME + " TEXT, " +
                    COLUMN_CREATION_TIME + " INTEGER" +
                    ");";
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOT_ANIMATIONS);
        onCreate(db);
    }

    public void addBootAnimationAsync(final BootAnimation bootAnimation, final DatabaseWriteCallback callback) {
        executor.execute(() -> {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(COLUMN_VIDEO_PATH, bootAnimation.getVideoPath());
                values.put(COLUMN_ZIP_PATH, bootAnimation.getZipPath());
                values.put(COLUMN_VIDEO_FILE_NAME, bootAnimation.getVideoFileName());
                values.put(COLUMN_ZIP_FILE_NAME, bootAnimation.getZipFileName());
                values.put(COLUMN_CREATION_TIME, bootAnimation.getCreationTime());

                db.insert(TABLE_BOOT_ANIMATIONS, null, values);
                mainThreadHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    @SuppressLint("Range")
    public void getAllBootAnimationsAsync(final BootAnimationCallback callback) {
        executor.execute(() -> {
            final List<BootAnimation> bootAnimations = new ArrayList<>();
            String selectQuery = "SELECT * FROM " + TABLE_BOOT_ANIMATIONS + " ORDER BY " + COLUMN_CREATION_TIME + " DESC";

            // Using try-with-resources ensures the cursor is closed automatically
            try (Cursor cursor = this.getReadableDatabase().rawQuery(selectQuery, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        BootAnimation bootAnimation = new BootAnimation();
                        bootAnimation.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                        bootAnimation.setVideoPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VIDEO_PATH)));
                        bootAnimation.setZipPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_PATH)));
                        bootAnimation.setVideoFileName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VIDEO_FILE_NAME)));
                        bootAnimation.setZipFileName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_FILE_NAME)));
                        bootAnimation.setCreationTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME)));
                        bootAnimations.add(bootAnimation);
                    } while (cursor.moveToNext());
                }
                mainThreadHandler.post(() -> callback.onComplete(bootAnimations));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    // Delete BootAnimation from db
    public void deleteBootAnimationAsync(final int id, final DatabaseWriteCallback callback) {
        executor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();

                // Delete the row with the specified id
                int deletedRows = db.delete(
                        TABLE_BOOT_ANIMATIONS,
                        "id = ?",
                        new String[]{String.valueOf(id)}
                );

                // Post result to main thread
                if (deletedRows > 0) {
                    mainThreadHandler.post(callback::onSuccess);
                } else {
                    mainThreadHandler.post(() ->
                            callback.onError(new Exception("No row found with id = " + id))
                    );
                }

            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onError(e));
            } finally {
                if (db != null && db.isOpen()) {
                    // optional: close db if you manage it manually
                    db.close(); // only if you don't reuse the db
                }
            }
        });
    }
}