// @File: Bot.java
// @Author: Robert Randolph
// @Class: COSC 4730
// @Assignment: Homework06
// @Due: December 9th, 2019
// Draws all the team colors on the same team as you.

package com.robertrandolph.myclient;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class TeamColorsView extends View {

    private String TAG = "TeamColors_TextureView";
    private ArrayList<TeamColor> teamColors;
    private ArrayList<Integer> PIDs;
    private Paint paint;
    private int team;

    public TeamColorsView(Context context) {
        super(context);
        setup();
    }
    public TeamColorsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }
    public TeamColorsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }
    public TeamColorsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setup();
    }

    private void setup() {
        Log.d(TAG, "Setting up teamcolors");
        // Init
        paint = new Paint();
        teamColors = new ArrayList<>();
        PIDs = new ArrayList<>();
    }

    // Adds a bot to draw
    public void addTeamColor(int PID, int team, int r, int g, int b) {
        Log.d(TAG, "Adding team color");
        Log.wtf(TAG, "Team: " + this.team);
        Log.d(TAG, "Got: " + team);
        if (PIDs.isEmpty()) {
            this.team = team;
        }
        if (this.team == team && !PIDs.contains(PID)) {
            PIDs.add(PID);
            teamColors.add(new TeamColor(r, g, b));
            invalidate();
        }
    }

    // Resets team colors
    public void reset() {
        Log.d(TAG, "Resetting team colors");
        PIDs.clear();
        teamColors.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "Drawing team colors");
        // Init
        int size = getHeight() / 2;
        int width = getWidth();
        int y = 0;
        int offset = 0;

        // Drawing team colors
        int it = 0;
        for (TeamColor color : teamColors) {
            paint.setColor(Color.rgb(color.r, color.g, color.b));
            if (size + offset > width) {
                y = size;
                offset = 0;
            }
            canvas.drawRect(0 + offset, y, size + offset, y + size, paint);
            offset += size;
        }
    }
}
