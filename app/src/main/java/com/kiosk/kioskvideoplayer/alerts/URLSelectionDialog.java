package com.kiosk.kioskvideoplayer.alerts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.kiosk.kioskvideoplayer.R;

public class URLSelectionDialog extends AppCompatDialogFragment {

    private OnURLSelectedListener listener;

    public interface OnURLSelectedListener {
        void onURLSelected(String selectedURL);
    }

    public void setOnURLSelectedListener(OnURLSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Deleting videos..");

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_url_layout, null);

        builder.setView(view)
                .setTitle("Seleccionar")
                .setPositiveButton("OK", (dialog, which) -> {
                    RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    if (selectedId != -1) {
                        RadioButton selectedRadioButton = view.findViewById(selectedId);
                        String selectedURL = selectedRadioButton.getText().toString();
                        String screenName = convertPantallaToScreen(selectedURL);
                        if (listener != null) {
                            listener.onURLSelected("https://apantalla.cortier.net/" + screenName.toLowerCase() + "/");
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        Button deleteOldVideo = view.findViewById(R.id.deleteOldVideo);
        deleteOldVideo.setOnClickListener(v -> {
            progressDialog.show();
//            StorageUtil.deleteVideos(10, false);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            }, 4000);
        });
//        for (int i = 0; i < Constant.arrayOfURLs.length; i++) {
//            RadioButton radioButton = (RadioButton) inflater.inflate(R.layout.list_item, null);
//            radioButton.setText(extractScreenName(Constant.arrayOfURLs[i]));
//            radioButton.setId(i);
//            radioGroup.addView(radioButton);
//
//            // Check if the current URL matches the default selection URL
//            if (Constant.arrayOfURLs[i].equals(MainActivityPresenter.BASE_URL)) {
//                radioButton.setChecked(true); // Set the RadioButton as checked
//            }
//        }

        return builder.create();
    }

    public static String convertPantallaToScreen(String input) {
        if (input.startsWith("Pantalla ")) {
            String number = input.substring(9); // Extract the number part after "Pantalla "
            return "screen" + number;
        } else {
            return input; // Return the input as is if it doesn't match the pattern
        }
    }


    public String extractScreenName(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1]; // Get the last part of the URL
        String[] screenParts = lastPart.split("screen");

        if (screenParts.length > 1) {
            return "Pantalla " + screenParts[1]; // Extracted screen name
        } else {
            return ""; // No screen name found
        }
    }

}
