package com.koisk.videokiosk.alerts;

import android.app.AlertDialog;
import android.content.Context;

public class AlertDialogUtils {

    public static void showInternetConnectionDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Se requiere conexión a internet");
        builder.setMessage("Se requiere conexión a internet para configurar la aplicación");

        builder.setPositiveButton("OK", (dialog, which) -> {
            // Optional: Add action when OK button is clicked
            dialog.dismiss(); // Dismiss the dialog
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
