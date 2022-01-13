/*
@File: CameraListener.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 02
@Due: March 2nd 2020
@Description: Communication vector for the CameraFragment/Preview and MainActivity
 */

package com.robertrandolph.mappicture;

import android.graphics.Bitmap;

public interface CameraListener {
    public void pictureTaken(Bitmap bitmap);
}
