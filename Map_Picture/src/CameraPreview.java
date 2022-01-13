/*
@File: CameraPreview.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: Handles the camera preview while taking a picture.
 */

package com.robertrandolph.mappicture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraPreview extends SurfaceView implements ImageReader.OnImageAvailableListener {

    // TAG
    private static final String TAG = "Camera Preview";

    // Values
    private Context context;
    private String cameraID;    // Camera to be used

    // Listeners & Handlers
    private CameraListener cameraListener;
    private Handler backgroundHandler;

    // Surfaces
    List<Surface> outputSurfaces;

    // Camera
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallBack;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilderCamera, captureRequestBuilderPreview;

    // Image
    private ImageReader imageReader;


    // Constructor
    public CameraPreview(Context context, String cameraID) {
        // Init
        super(context);
        Log.d(TAG, "New Camera Preview (Constructor)");
        this.context = context;
        this.cameraID = cameraID;   // Camera to be used
        setUpCamera();
    }

    // Sets the camera listener
    public void setCameraListener(CameraListener listener) {
        Log.d(TAG, "Setting Camera Listener (Preview)");
        cameraListener = listener;
    }

    //============================================================\\
    // Camera
    //============================================================\\

    // Takes a picture (starting capture session)
    public void takePicture() {
        Log.d(TAG, "Taking picture");
        try {
            cameraDevice.createCaptureSession(outputSurfaces, cameraCaptureSessionStateCallBack, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // Opens the camera and sets up camera objects
    protected void openCamera() {
        Log.d(TAG, "Opening Camera...");

        // Checking for permissions
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            // Opening camera
            Log.d(TAG, "Opened Camera");
            manager.openCamera(cameraID, cameraDeviceStateCallback, null);

            // Getting camera properties
            Log.d(TAG, "Getting camera characteristics");
            cameraCharacteristics = manager.getCameraCharacteristics(cameraID);

            // Getting image dimensions if possible, else uses default size 640/480
            Log.d(TAG, "Getting camera image dimensions");
            Size[] sizes = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int width = 640, height = 480;
            if (sizes != null && sizes.length > 0) {
                width = sizes[0].getWidth();
                height = sizes[0].getHeight();
            }

            // Setting up image surfaces
            Log.d(TAG, "Setting up image surfaces");
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReader.getSurface());

            // Setting up image available feedback
            Log.d(TAG, "Setting up image preview feedback");
            HandlerThread thread = new HandlerThread("Camera Preview");
            thread.start();
            backgroundHandler = new Handler(thread.getLooper());
            imageReader.setOnImageAvailableListener(this, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to open camera");
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "Finished opening camera");
    }

    // Closes the camera
    protected void closeCamera() {
        Log.d(TAG, "Closing Camera");
        cameraDevice.close();
    }

    // Setup the parts necessary for the camera
    private void setUpCamera() {
        Log.d(TAG, "Setting up camera parts");

        Log.d(TAG, "Setting up state call back");
        cameraDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Log.d(TAG + " | SCB", "Camera Opened\nStarting Preview");
                cameraDevice = camera;
                startPreview();

                Log.d(TAG, "Preparing to take picture");
                try {
                    captureRequestBuilderCamera = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                // Setting surface target
                Log.d(TAG, "Setting surface target for picture");
                captureRequestBuilderCamera.addTarget(imageReader.getSurface());
                captureRequestBuilderCamera.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                // Setting orientation
                Log.d(TAG, "Setting orientation");
                int deviceOrientation = getResources().getConfiguration().orientation;
                captureRequestBuilderCamera.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(cameraCharacteristics, deviceOrientation));
            }
            public void onDisconnected(CameraDevice camera) {}  // Ignored
            public void onError(CameraDevice camera, int error) {}  // Ignored
        };

        //============================================================\\

        Log.d(TAG, "Setting up camera capture session state call back");
        cameraCaptureSessionStateCallBack = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                try {
                    session.capture(captureRequestBuilderCamera.build(), null, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            public void onConfigureFailed(CameraCaptureSession session) {} // Ignored
        };
    }

    //============================================================\\
    // Camera Preview
    //============================================================\\

    // Starts the preview for the camera
    private void startPreview() {
        Log.d(TAG, "Starting preview");

        // Checking if there is a camera device
        if (cameraDevice == null) {
            Log.e(TAG, "Preview startup failed");
            return;
        }

        // Getting the surface and setting up preview
        Surface surface = getHolder().getSurface();
        try {
            captureRequestBuilderPreview = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequestBuilderPreview.addTarget(surface);

        // Getting the capture session and updating preview
        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    updatePreview();
                }
                public void onConfigureFailed(CameraCaptureSession session) {} // Ignored
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void updatePreview() {
        Log.d(TAG, "Updating preview");

        // Checking if camera device is null
        // Checking if there is a camera device
        if (cameraDevice == null) {
            Log.e(TAG, "Preview startup failed");
            return;
        }

        // Setting up camera preview feedback
        Log.d(TAG, "Setting up camera preview feedback");
        captureRequestBuilderPreview.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("Camera Preview");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        // Updating preview
        Log.d(TAG, "Repeating update requests");
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilderPreview.build(), null, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //============================================================\\
    // Image
    //============================================================\\

    // helper method so set the picture orientation correctly.  This doesn't set the header in jpeg
    // instead it just makes sure the picture is the same way as the phone is when it was taken.
    // Code snippet from Jims examples
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        Log.d(TAG, "Getting JPEG Orientation");
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    // Handles the image taken by the camera
    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.d(TAG, "Image available");

        // Getting image and turning into bitmap
        Image image = null;
        image = reader.acquireLatestImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        image.close();

        // Sending picture to mainactivity for processing
        cameraListener.pictureTaken(bitmap);
    }
}
