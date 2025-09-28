package bootanimation.zip.bootzipcreator.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.models.BootAnimation;
import bootanimation.zip.bootzipcreator.others.CustomMethods;

public class DialogHistoryItemContextMenu extends Dialog {

    private final BootAnimation bootAnimation;
    private OnActionListener listener;

    public interface OnActionListener {
        void onDelete(BootAnimation bootAnimation);
        void onShare(BootAnimation bootAnimation);
    }

    public DialogHistoryItemContextMenu(@NonNull Context context, BootAnimation bootAnimation) {
        super(context);
        this.bootAnimation = bootAnimation;
    }

    public void setOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_history_item_context_menu);

        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView fileNameTV = findViewById(R.id.fileNameTV);
        TextView fileSizeTV = findViewById(R.id.fileSizeTV);
        TextView filePathTV = findViewById(R.id.filePathTV);
        TextView createdAtTV = findViewById(R.id.createdAtTV);
        Button shareButton = findViewById(R.id.shareButton);
        Button deleteButton = findViewById(R.id.deleteButton);

        fileNameTV.setText(bootAnimation.getZipFileName());
        String filePath = bootAnimation.getZipPath();
        File zipFile = new File(filePath);

        if (zipFile.exists()) {
            fileSizeTV.setText(CustomMethods.formatFileSize(zipFile.length()));
        } else {
            fileSizeTV.setText(getContext().getString(R.string.file_not_found));
        }

        filePathTV.setText(filePath);
        createdAtTV.setText(CustomMethods.formatTimestamp(bootAnimation.getCreationTime(), false));

        shareButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShare(bootAnimation);
            }
            dismiss();
        });

        deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(bootAnimation);
            }
            dismiss();
        });
    }

    @Override
    public void show() {
        super.show();
    }

}
