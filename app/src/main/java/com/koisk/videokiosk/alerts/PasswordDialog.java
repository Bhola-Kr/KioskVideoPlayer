package com.koisk.videokiosk.alerts;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.widget.EditText;


public class PasswordDialog {

    public interface PasswordDialogListener {
        void onPasswordEntered(String password);
    }

    public static void showPasswordDialog(Context context, final PasswordDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Contraseña Inicial");

        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        input.setHint("0000");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredPassword = input.getText().toString().trim();
            listener.onPasswordEntered(enteredPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }
}
