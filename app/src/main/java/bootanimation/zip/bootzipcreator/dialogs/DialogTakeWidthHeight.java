package bootanimation.zip.bootzipcreator.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.text.Editable;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import bootanimation.zip.bootzipcreator.R;

public class DialogTakeWidthHeight {

    private final Dialog dialog;
    private final TextInputEditText widthInputEditText;
    private final TextInputEditText heightInputEditText;

    public DialogTakeWidthHeight(Activity activity, OnOkayClickListener listener) {
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_take_width_height);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        // Make dialog width match parent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextInputLayout widthInput = dialog.findViewById(R.id.widthInput);
        TextInputLayout heightInput = dialog.findViewById(R.id.heightInput);
        widthInputEditText = dialog.findViewById(R.id.widthInputEditText);
        heightInputEditText = dialog.findViewById(R.id.heightInputEditText);

        MaterialButton cancelBtn = dialog.findViewById(R.id.cancelBtn);
        MaterialButton okayBtn = dialog.findViewById(R.id.okayBtn);

        cancelBtn.setOnClickListener(view -> listener.onCancelClick(dialog));

        okayBtn.setOnClickListener(view -> {

            Editable widthText = widthInputEditText.getText();
            Editable heightText = heightInputEditText.getText();

            if (widthText != null && heightText != null) {

                String widthString = widthText.toString();
                String heightString = heightText.toString();

                if (widthString.isEmpty()) {
                    widthInput.setError("Width and height must be greater than 0");
                    return;
                }

                if (heightString.isEmpty()) {
                    heightInput.setError("Width and height must be greater than 0");
                    return;
                }

                int width = Integer.parseInt(widthText.toString());
                int height = Integer.parseInt(heightText.toString());

                if (width <= 0) {
                    widthInput.setError("Width and height must be greater than 0");
                    return;
                }

                if (height <= 0) {
                    heightInput.setError("Width and height must be greater than 0");
                    return;
                }

                widthInput.setError(null);
                heightInput.setError(null);

                listener.onOkayClick(width, height);
                dialog.dismiss();
            } else {
                widthInput.setError("Width and height must be greater than 0");
                heightInput.setError("Width and height must be greater than 0");
            }
        });
    }

    public void setDefaultValues(int width, int height) {
        widthInputEditText.setText(String.valueOf(width));
        heightInputEditText.setText(String.valueOf(height));
    }

    public void show() {
        dialog.show();
    }

    public interface OnOkayClickListener {
        void onOkayClick(int width, int height);
        void onCancelClick(Dialog dialog);
    }
}
