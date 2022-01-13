/*
@File: MainActivity.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 03
@Due: March 25, 2020
@Description: ---
 */

package com.robertrandolph.tictactoe_connected;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Tag
    private static final String TAG = "MainActivity";

    // Network Connection Values
    public static final String NAME = "TicTacToe_Connected";
    public static final UUID MYUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");  // Bluetooth

    // Fragments
    private TicTacToeFragment board;

    // Widgets
    private BottomNavigationView botNavView;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Inflating view");
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Setting up fragments
        board = new TicTacToeFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, board).commit();
        }
    }
}
