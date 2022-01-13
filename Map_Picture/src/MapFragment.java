/*
@File: MapFragment.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: Handles the Map and its events.
When the app is started it will zoom in on the current location.
When a picture is taken, a marker will appear at the user's current location.
When the marker is pressed the picture taken at that location will appear.
 */

package com.robertrandolph.mappicture;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // TAG
    private static final String TAG = "Map Fragment";

    // Listener
    MapListener mapListener = null; // Coms with MainActivity

    // Map
    private GoogleMap map;
    private Location lastLocation;

    public MapFragment() {} // Ignored

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflating layout
        Log.d(TAG, "Inflating layout");
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Setting up map
        Log.d(TAG, "Setting up map");
        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        // Retuning view
        return view;
    }

    // Updates the map by focusing the camera on the user's current location.
    public void updateMap(Location location) {
        Log.d(TAG, "Updating map");
        lastLocation = location;
        if (location != null) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 17));
            Log.d(TAG, "Location set");
        }
        else {
            Log.d(TAG, "Location was null");
        }
    }

    // The map is ready to use. Initializes settings and configurations.
    // Attempts to focus the camera on the user's current location.
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "Map is ready");

        // setting up map
        Log.d(TAG, "Setting up map");
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        // Setting listener for the map when markers are clicked
        map.setOnMarkerClickListener(this);

        // Attempting to focus camera on current location.
        if (mapListener != null) {
            mapListener.requestLocation();
        }

    }

    // Adds a marker to the users current location
    public void addMarker(Bitmap bitmap) {
        Log.d(TAG, "Adding marker to map");
        Log.d(TAG, "Focusing camera on current position");
        if (mapListener != null) {
            mapListener.requestLocation();
        }

        Log.d(TAG, "Getting lat and long");
        LatLng ll;
        if (lastLocation != null) {
            ll = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        }
        else {
            Log.d(TAG, "Current/last location unknown");
            return;
        }

        // Adding marker
        Log.d(TAG, "Adding Marker");
        map.addMarker(new MarkerOptions()
        .position(ll)
        .title("A Marker"))
        .setTag(bitmap);
    }

    // Sets the map listener. Used to communicate with the main activity.
    public void setMapListener(MapListener listener) {
        Log.d(TAG, "Setting map listener");
        mapListener = listener;
    }

    // Handles when a marker is clicked
    // Displays the image the marker is holding using a dialog
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "Marker clicked");

        // Retrieving bitmap
        Bitmap bitmap = (Bitmap) marker.getTag();

        // Displaying bitmap
        DialogFragment.newInstance(bitmap).show(getActivity().getFragmentManager(), "Image Dialog");
        return false;
    }
}