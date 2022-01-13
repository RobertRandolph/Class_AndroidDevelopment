/*
@File: MainActivity.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: A program that uses google maps, location services, and a camera.
The user can take a picture and it will create a marker on google maps at the user's current
location. When the marker is pressed, the picture taken there will appear.
The camera and map are split into separate fragments, with the main activity displaying one of them.
The fragments can be traversed through a bottom navigation view.
==Note== : The fragments are both in the same container and are created only once since they
            aren't being removed, but simply hidden or shown. This was done to get around
            a weird issue where the google map tiles weren't displaying on fragment change.
@Mentions: Used code snippets from Jim's various examples.
 */

package com.robertrandolph.mappicture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, MapListener, CameraListener {

    // TAG
    private static final String TAG = "Main Activity";

    // For checking permissions
    public static final int REQUEST_ACCESS_onConnected = 1;

    // Fragments
    private MapFragment mapFragment;
    private CameraFragment cameraFragment;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private Location lastLocation = null;

    @Override
    @SuppressLint("SourceLockedOrientationActivity")
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Creating main activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Setting up fragments
        Log.d(TAG, "Setting up fragments");
        mapFragment = new MapFragment();
        mapFragment.setMapListener(this);
        cameraFragment = new CameraFragment();
        cameraFragment.setCameraListener(this);

        // Setting initial layout
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, mapFragment).add(R.id.container, cameraFragment).hide(cameraFragment).commit();
        }

        // Setting up bottom navigation
        Log.d(TAG, "Setting up bottom navigation");
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(this);

        // Setting up location
        setupLocation();
    }

    //============================================================\\
    // Location
    //============================================================\\

    // Sets up location structure
    private void setupLocation() {
        Log.d(TAG, "Setting up location");
        Log.d(TAG, "Getting fused Location Client");
        fusedLocationClient = new FusedLocationProviderClient(this);

        //============================================================\\

        Log.d(TAG, "Getting settings client");
        settingsClient = new SettingsClient(this);

        //============================================================\\

        Log.d(TAG, "Creating Location Request");
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //============================================================\\

        Log.d(TAG, "Creating Location Callback");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                lastLocation = locationResult.getLastLocation();
                // @TODO do stuff when location is received. (when looping on loc finds)
            }
        };

        //============================================================\\

        Log.d(TAG, "Building Location Settings Request");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    // Gets the current location of the phone.
    // First checks if permissions were granted; if they aren't, stops and requests permissions.
    // When location is updated, updates google maps to focus on location.
    private void getLocation() {
        Log.d(TAG, "Getting location");
        // Checking for permissions
        if(!checkPermissions()) return;

        // Getting location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Log.d(TAG, "Location found");
                        // Checking if no location was found
                        if (location == null) {
                            Log.w(TAG, "LocationSuccess:null");
                            return;
                        }

                        // Storing location and updating google maps
                        lastLocation = location;
                        mapFragment.updateMap(lastLocation);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.w(TAG, "LocationFailed", e);
                    }
                });
    }

    //============================================================\\
    // Listener call backs
    //============================================================\\

    // Handles when the MapFragment requests a location update
    @Override
    public void requestLocation() {
        Log.d(TAG, "Location Requested");
        getLocation();
    }

    // Handles when the picture from the camera fragment/preview becomes available.
    @Override
    public void pictureTaken(final Bitmap bitmap) {
        Log.d(TAG, "Picture was take and received");
        // Needs to be run on the main thread since this is called on a side thread
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Adding marker, and switching view.
                mapFragment.addMarker(bitmap);
                BottomNavigationView nav = findViewById(R.id.nav_view);
                nav.setSelectedItemId(R.id.mapFragment);
            }
        });
    }

    // Handles events for when the bottom navigation view buttons are selected.
    // Changes between fragments depending on which item was selected.
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(TAG, "Navigation Item selected");
        switch (item.getItemId()) {
            case R.id.mapFragment:
                getSupportFragmentManager().beginTransaction().hide(cameraFragment).show(mapFragment).commit();
                cameraFragment.closeCamera();
                return true;
            case R.id.cameraFragment:
                getSupportFragmentManager().beginTransaction().hide(mapFragment).show(cameraFragment).commit();
                cameraFragment.openCamera();
                return true;
        }
        return false;
    }

    //============================================================\\
    // Permissions
    //============================================================\\

    // Checks permissions, and sends request if they are missing.
    private boolean checkPermissions() {
        Log.d(TAG, "Checking Permissions");
        // Checking permissions
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            Log.d(TAG, "Asking for permissions");
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA},
                    MainActivity.REQUEST_ACCESS_onConnected);
            return false;
        }
        Log.d(TAG, "Permissions Good");
        return true;
    }

    // Permission request completed
    // If Permissions were denied, closes the app.
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Init
        Log.d(TAG, "onRequest result called.");
        boolean coarse = false, fine = false, camera = false;

        // Received results, checking if permissions were granted
        Log.d(TAG, "Checking if permissions were granted");
        for (int i = 0; i < grantResults.length; i++) {
            if ((permissions[i].compareTo(Manifest.permission.ACCESS_COARSE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                coarse = true;
            else if ((permissions[i].compareTo(Manifest.permission.ACCESS_FINE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                fine = true;
            else if ((permissions[i].compareTo(Manifest.permission.CAMERA) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                camera = true;
        }

        // Checking if permissions were granted and taking action based on results.
        Log.d(TAG, "Received response for permission requests.");
        if (coarse && fine && camera) {
            Log.d(TAG, "Permissions were granted");
            getLocation();

        } else {
            // Permissions denied.
            Log.d(TAG, "Permissions were not granted, closing app");
            finish();
        }
    }
}
