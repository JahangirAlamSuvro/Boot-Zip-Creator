package bootanimation.zip.bootzipcreator.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.others.BootAnimationCreator;

public class DialogProcessingVideo {

    private final Dialog dialog;
    private final Activity activity;
    private final TextView statusTextView;
    private final TextView progressText;
    private final TextView footerInfoTV;
    private final ProgressBar circularProgressBar;
    private final Button cancelButton;
    private int videoWidth, videoHeight, fps, totalFrames;
    private float startFrame, endFrame;
    private Uri videoUri;

    public DialogProcessingVideo(Activity myActivity) {
        activity = myActivity;
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_processing_view);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        statusTextView = dialog.findViewById(R.id.statusTextView);
        progressText = dialog.findViewById(R.id.progressText);
        footerInfoTV = dialog.findViewById(R.id.footerInfoTV);
        circularProgressBar = dialog.findViewById(R.id.circularProgressBar);
        cancelButton = dialog.findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(v -> {
            FFmpeg.cancel();
            dialog.dismiss();
            Toast.makeText(activity, "Processing Canceled", Toast.LENGTH_SHORT).show();
        });
    }

    public void setVideoInfo(int width, int height, int fps, int totalFrames) {
        videoWidth = width;
        videoHeight = height;
        this.fps = fps;
        this.totalFrames = totalFrames;
    }

    public void setVideoUri(Uri uri) {
        videoUri = uri;
    }

    public void setTrimRange(float start, float end) {
        startFrame = start;
        endFrame = end;
    }

    public void startProcessing() {
        BootAnimationCreator.ProgressListener listener = new BootAnimationCreator.ProgressListener() {
            @Override
            public void onProgress(int progress) {
                updateProgress(progress);
            }

            @Override
            public void onStatusUpdate(String status) {
                updateStatus(status);
            }

            @Override
            public void onComplete(String zipFilePath) {
                updateStatus("Completed! Saved to:\n" + zipFilePath);
                cancelButton.setText(R.string.close);
                cancelButton.setOnClickListener(v -> dialog.dismiss());
                footerInfoTV.setText(activity.getString(R.string.generated_success_message));
                footerInfoTV.setTextColor(activity.getColor(R.color.green));
            }

            @Override
            public void onError(String message) {
                updateStatus("Error: " + message);
                cancelButton.setText(R.string.close);
                cancelButton.setOnClickListener(v -> dialog.dismiss());
            }
        };

        BootAnimationCreator creator = new BootAnimationCreator(activity, videoUri, videoWidth, videoHeight, fps, totalFrames, startFrame, endFrame, listener);
        creator.create();
        dialog.show();
    }

    public void updateProgress(int progress) {
        circularProgressBar.setProgress(progress);
        String progressTextStr = progress + "%";
        progressText.setText(progressTextStr);
    }

    public void updateStatus(String status) {
        statusTextView.setText(status);
    }
}
