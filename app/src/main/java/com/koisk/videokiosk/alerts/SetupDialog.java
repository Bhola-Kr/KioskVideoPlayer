package com.koisk.videokiosk.alerts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.koisk.videokiosk.R;


public class SetupDialog extends AppCompatDialogFragment {

    private SetupDialogListener setupDialogListener;
    private Dialog dialog; // Added to hold reference to the dialog instance

    public interface SetupDialogListener {
        void onLauncherSelected();

        void onLinkSelected();
    }

    // Static method to show the dialog with a callback listener
    public static void showDialog(Context context, SetupDialogListener listener) {
        SetupDialog setupDialog = new SetupDialog();
        setupDialog.setupDialogListener = listener;
        setupDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "SetupDialog");
    }

    // Method to dismiss the dialog
    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.setup_dialog_layout, null);
        builder.setView(view);

        Button selectLinkButton = view.findViewById(R.id.selectLinkButton);
        Button selectLauncherButton = view.findViewById(R.id.selectLauncherButton);

        selectLinkButton.setOnClickListener(v -> {
            if (setupDialogListener != null) {
                setupDialogListener.onLinkSelected();
            }
            dismissDialog(); // Dismiss dialog when the link is selected
        });

        selectLauncherButton.setOnClickListener(v -> {
            if (setupDialogListener != null) {
                setupDialogListener.onLauncherSelected();
            }
            dismissDialog(); // Dismiss dialog when the launcher is selected
        });

        dialog = builder.create(); // Save the dialog instance
        return dialog;
    }
}
