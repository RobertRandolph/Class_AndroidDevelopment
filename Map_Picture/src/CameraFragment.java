/*
@File: CameraFragment.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: Handles the camera and picture taking events.
The camera is displayed in it's own fragment. The picture preview of the camera takes up the
entire space, and a small button at the bottom allows the user to take a picture.
When the picture is taken, it is sent to the main activity to be stored and handled.

Ensures that the camera is only ever opened once, and pictures do not overlap between captures
(When the capture button is pressed multiple times)
 */

package com.robertrandolph.mappicture;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CameraFragment extends Fragment implements View.OnClickListener {

    // TAG
    private static final String TAG = "Camera Fragment";

    // Values / Control
    private String cameraID = "";
    private boolean taking = false; // Prevents multiple capture sessions from overlapping
    private boolean opened = false; // Ensures that the camera is only opened once.

    // Listeners
    CameraListener cameraListener = null;

    // Widgets
    private FrameLayout cameraPreviewContainer;
    private FloatingActionButton btnTakePicture;

    // Camera
    CameraPreview cameraPreview;

    public CameraFragment() {}  // Ignored

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflating view
        Log.d(TAG, "Inflating view");
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        // Getting and initializing widgets
        cameraPreviewContainer = view.findViewById(R.id.cameraPreviwContainer);
        btnTakePicture = view.findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(this);

        // Setting up camera
        Log.d(TAG, "Getting camera ID");
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraID = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to get camera ID");
            e.printStackTrace();
        }

        // Adding camera preview to container
        Log.d(TAG, "Creating camera preview and adding to container.");
        cameraPreview = new CameraPreview(getActivity().getApplicationContext(), cameraID);
        cameraPreview.setCameraListener(cameraListener);
        cameraPreviewContainer.addView(cameraPreview);

        return view;
    }

    // Sets the camera listener
    public void setCameraListener(CameraListener listener) {
        Log.d(TAG, "Setting camera listener (Fragment)");
        cameraListener = listener;
    }

    //============================================================\\
    // Camera
    //============================================================\\

    // Opens the camera
    protected void openCamera() {
        Log.d(TAG, "Passing Open Camera Message to Preview");
        // Checking if already opened
        // If so, simply returns
        if (opened) {
            Log.d(TAG, "Camera already opened");
            return;
        }

        // Opening camera & Setting control
        cameraPreview.openCamera();
        taking = false;
        opened = true;
    }

    // Closes the camera
    protected void closeCamera() {
        Log.d(TAG, "Passing Close Camera Message to Preview");
        cameraPreview.closeCamera();
        opened = false;
    }

    //============================================================\\
    // Listener call backs
    //============================================================\\

    // Handles when the floating action button is pressed.
    // Will proceed to take a picture.
    @Override
    public void onClick(View v) {
        Log.d(TAG, "Capture button clicked; Taking picture");
        // Checking if already in the process of taking a picture
        // IF so, simply returns (cancels overlapping capture)
        if (taking) {
            Log.d(TAG, "Already taking a picture");
            return;
        }

        // Taking the picture
        taking = true;
        cameraPreview.takePicture();
    }
}
