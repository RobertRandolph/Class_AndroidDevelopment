/*
@File: DialogFragment.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: Dialog that will display a given bitmap
 */

package com.robertrandolph.mappicture;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

public class DialogFragment extends android.app.DialogFragment {

    // TAG
    private static final String TAG = "Dialog";

    // Image
    private Bitmap bitmap;

    // Constructor
    public DialogFragment() {}  // Ignored

    // Creating new dialog
    public static DialogFragment newInstance(Bitmap bitmap) {
        Log.d(TAG, "New Dialog Instance");
        DialogFragment dialog = new DialogFragment();
        dialog.bitmap = bitmap;
        return dialog;
    }

    // Creating the dialog
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "Init Dialog");

        // Inflating dialog
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.image_dialog, null);

        // Getting image
        final ImageView image = view.findViewById(R.id.imageView);
        image.setImageBitmap(bitmap);

        // Building dialog
        Log.d(TAG, "Building dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.ThemeOverlay_AppCompat_Dialog));
        builder.setView(view)
                .setTitle("Image")
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        Log.d(TAG, "Returning dialog");
        return builder.create();
    }
}