package bootanimation.zip.bootzipcreator.others;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomMethods {
    public static String getVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

//    ----------------------------------------------------------

    public static String getCurrentTimestamp() {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        return outputFormat.format(new Date());
    }

    //    ----------------------------------------------------------
    public static String formatTimestamp(long timestamp, boolean inSeconds) {
        if (inSeconds) {
            timestamp = timestamp * 1000L; // convert seconds to milliseconds
        }

        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }
//    ----------------------------------------------------------

    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 Bytes";
        final String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        double size = sizeInBytes / Math.pow(1024, digitGroups);
        return String.format(Locale.US, "%.2f %s", size, units[digitGroups]);
    }
//    ----------------------------------------------------------

    public static void showSimpleDialog(Context context, String title, String message, int drawableResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setIcon(drawableResId);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
