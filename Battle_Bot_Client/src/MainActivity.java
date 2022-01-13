// @File: MainActivity.java
// @Author: Robert Randolph
// @Class: COSC 4730
// @Assignment: Homework06
// @Due: December 9th, 2019
// Used Joystick at https://github.com/controlwear/virtual-joystick-android

package com.robertrandolph.myclient;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity implements Button.OnClickListener {

    private final String TAG = "MAIN";

    // Defaults
     private final String DEFAULT_HOST = "10.216.217.131";
//    private final String DEFAULT_HOST = "10.0.2.2";
    private final String DEFAULT_PORT = "3012";

    // Widgets
    private ViewFlipper viewFlipper;
    // Connect
    private EditText host, port;
    private TextView points_lb, armor_lb, power_lb, scan_lb, connection_status_lb;
    private Button connect, armor_plus, armor_minus, power_plus, power_minus, scan_plus, scan_minus;
    // Play
    private JoystickView play_joystick_move, play_joystick_fire;
    private Button play_shoot_powerup;
    private TextView play_hp, play_moveCount, play_fireCount, play_team_lb, play_powerup, play_status;
    private TeamColorsView teamColorsView;

    // Network Status
    private String connection_status;
    private boolean connected;

    // Game & Arena Info
    private int PID;
    private int arena_width, arean_height;
    private int numBots, team;

    // Bot
    // Setup
    private String name = "Wolf_Sensei";
    private int points, armor_pts, power_pts, scan_pts; // Assigned pts
    // Stats
    private int armor_value, moveRate, scanDistance, bulletPower, rateOfFire, bulletDistance;
    private int red, green, blue;                       // Color
    // Status
    private int xPos, yPos, moveCount, shotCount, HP;   // Current values
    // Actions
    private boolean action_move;
    private int moveX, moveY;
    private boolean action_fire;
    private int fire_angle;
    private boolean action_shootPowerup;    // Uses one scan
    // Scan
    private boolean powerup_found;
    private int powerup_xPos;
    private int powerup_yPos;
    private int messages_till_scan;         // Number of messages till scan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Views
        viewFlipper = findViewById(R.id.viewFlipper);
        teamColorsView = findViewById(R.id.play_team_colors);

        // Network & Bot Setup
        connected = false;
        connection_status = "";
        points = 5;
        armor_pts = 1;
        power_pts = 1;
        scan_pts = 1;

        // Getting widgets
        // Connection
        // Network
        host = findViewById(R.id.host);
        host.setText(DEFAULT_HOST);
        port = findViewById(R.id.port);
        port.setText(DEFAULT_PORT);
        connection_status_lb = findViewById(R.id.connection_status);
        connect = findViewById(R.id.connect);
        connect.setOnClickListener(this);

        // Bot Setup
        points_lb = findViewById(R.id.points_lb);
        armor_lb = findViewById(R.id.armor_pts);
        power_lb = findViewById(R.id.power_pts);
        scan_lb = findViewById(R.id.scan_pts);
        // Bot Value Modifiers
        armor_plus = findViewById(R.id.armor_plus);
        armor_plus.setOnClickListener(this);
        armor_minus = findViewById(R.id.armor_minus);
        armor_minus.setOnClickListener(this);
        power_plus = findViewById(R.id.power_plus);
        power_plus.setOnClickListener(this);
        power_minus = findViewById(R.id.power_minus);
        power_minus.setOnClickListener(this);
        scan_plus = findViewById(R.id.scan_plus);
        scan_plus.setOnClickListener(this);
        scan_minus = findViewById(R.id.scan_minus);
        scan_minus.setOnClickListener(this);

        // Bot Actions
        play_joystick_move = findViewById(R.id.play_joystick_move);
        play_joystick_move.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                Log.d(TAG, "Move Joystick");
                // Calculating move direction
                // X
                if (angle > 112.5 && angle < 247.5) moveX = -1;     // Left
                else if ((angle < 67.5 && angle >= 0) || (angle > 292.5 && angle <= 360)) moveX = 1;    // Right
                else moveX = 0;                                     // Not moveing left/right
                // Y
                if (angle > 22.5 && angle < 157.5) moveY = -1;      // Up
                else if (angle > 202.5 && angle < 337.5) moveY = 1; // Down
                else moveY = 0;                                     // Not moving up/down
                action_move = true;
            }
        });
        play_joystick_fire = findViewById(R.id.play_joystick_fire);
        play_joystick_fire.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                Log.d(TAG, "Fire Joystick");
                // Calculating fireing angle
                fire_angle = (450 - angle) % 360;
                action_fire = true;
            }
        });
        play_shoot_powerup = findViewById(R.id.play_shoot_powerup);
        play_shoot_powerup.setOnClickListener(this);
        // Bot Status
        play_hp = findViewById(R.id.play_hp);
        play_moveCount = findViewById(R.id.play_moveCount);
        play_fireCount = findViewById(R.id.play_shotCount);
        play_team_lb = findViewById(R.id.play_team_lb);
        play_powerup = findViewById(R.id.play_powerup);
        play_status = findViewById(R.id.play_status);

        // Init play area
        resetPlay();
    }

    // ================================================
    // UI Methods
    // ================================================

    // Updates the connection status
    private void updateConnectionStatus(final String status) {
        Log.d(TAG, "Updating Connection Status");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connection_status_lb.setText(status);
            }
        });
    }

    // Updates the play area bot status
    private void updateBotStatus() {
        Log.d(TAG, "Updating Bot Status");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                play_hp.setText("HP: " + HP);
                play_moveCount.setText("Stamina: " + moveCount);
                play_fireCount.setText("Reload: " + shotCount);
            }
        });
    }

    // Updates the play area powerup status
    private void updatePowerUpStatus(final String status) {
        Log.d(TAG, "Updating Power Up Status");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                play_powerup.setText("Power Up: " + status);
            }
        });
    }

    // Updates the play area status
    private void updatePlayStatus(final String status) {
        Log.d(TAG, "Updating Play Status");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                play_status.setText(status);
            }
        });
    }

    // Updates the team colors
    private void updateTeamColors(final int PID, final int team, final int r, final int g, final int b) {
        Log.d(TAG, "Updating team colors");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                teamColorsView.addTeamColor(PID, team, r, g, b);
            }
        });
    }

    // Switches between views
    private void switchView() {
        Log.d(TAG, "Switching Views");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewFlipper.showNext();
            }
        });
    }

    // ================================================
    // Utility Methods
    // ================================================

    // Resets the play area.
    private void resetPlay() {
        action_move = false;
        action_fire = false;
        action_shootPowerup = false;
        powerup_found = false;
        messages_till_scan = 0;

        teamColorsView.reset();
    }

    // Gets the angle between the bot and the given coordinates.
    private int getFireAngle(int x, int y) {
        return getAngle(xPos, yPos, x, y);
    }

    // Calculates the angle between two points, where point (x1, x2) is the origin of a sphere.
    // Returns the angle in degrees in relation to firing angle of server.
    private int getAngle(int x1, int y1, int x2, int y2) {
        int x = x2 - x1;
        int y = y2 - y1;
        int angle = (int)(180 / Math.PI * Math.atan2(-y, x));
        return (450 - angle) % 360;
    }

    // ================================================
    // User Input Methods
    // ================================================

    // Bot point change or bot action
    public void onClick(View v) {
        Log.d(TAG, "Button Pressed");

        // Determining pressed button
        if (v == connect) connectToServer();
        else if (v == armor_plus) modifyBot(armor_lb, true);
        else if (v == armor_minus) modifyBot(armor_lb, false);
        else if (v == power_plus) modifyBot(power_lb, true);
        else if (v == power_minus) modifyBot(power_lb, false);
        else if (v == scan_plus) modifyBot(scan_lb, true);
        else if (v == scan_minus) modifyBot(scan_lb, false);
        else if (v == play_shoot_powerup) action_shootPowerup = true;
    }

    // Modifies the bot values
    // If attempting to connected or connected to the server it won't modify.
    private void modifyBot(TextView value, boolean plus) {
        Log.d(TAG, "Modifying Bot...");

        // Checking if connected
        // If so, returns
        if (connected) {
            connection_status_lb.setText("Unable to modify bot | " + connection_status);
            return;
        }

        // Labels
        String pts = "Points Left: ";
        String hp = "Armor (HP): ";
        String dmg = "Power (DMG): ";
        String vision = "Scan (Vision): ";

        // Checking if adding value
        // If so, ensures there is a point to spend
        if (plus && points > 0) {
            if (value == armor_lb && armor_pts < 5) {
                Log.d(TAG, "Armor+ | Speed reduced.");
                connection_status_lb.setText("Armor+ | Speed reduced.");
                armor_pts += 1;
                armor_lb.setText(hp + armor_pts);
            }
            else if (value == power_lb && power_pts < 5) {
                Log.d(TAG, "Power+ | ROF & bullet distance reduced.");
                connection_status_lb.setText("Power- | ROF & bullet distance reduced.");
                power_pts += 1;
                power_lb.setText(dmg + power_pts);
            }
            else if (value == scan_lb && scan_pts < 5) {
                Log.d(TAG, "Scan+ | Scan distance increased.");
                connection_status_lb.setText("Scan+ | Scan distance increased.");
                scan_pts += 1;
                scan_lb.setText(vision + scan_pts);
            }
            else {
                Log.d(TAG, "Unable to allocate point | at max (5).");
                connection_status_lb.setText("Unable to allocate point | at max (5).");
                return;
            }
            points -= 1;
            points_lb.setText(pts + points);
        }
        // Checking if subtracting value
        // If so, ensures there is a point to remove
        else if (!plus && points < 5) {
            if (value == armor_lb && armor_pts > 1) {
                Log.d(TAG, "Armor- | Speed increased.");
                connection_status_lb.setText("Armor- | Speed increased.");;
                armor_pts -= 1;
                armor_lb.setText(hp + armor_pts);
            }
            else if (value == power_lb && power_pts > 1) {
                Log.d(TAG, "Power- | ROF & bullet distance increased.");
                connection_status_lb.setText("Power- | ROF & bullet distance increased.");
                power_pts -= 1;
                power_lb.setText(dmg + power_pts);
            }
            else if (value == scan_lb && scan_pts > 1) {
                Log.d(TAG, "Scan- | Scan distance decreased.");
                connection_status_lb.setText("Scan- | Scan distance decreased.");
                scan_pts -= 1;
                scan_lb.setText(vision + scan_pts);
            }
            else {
                Log.d(TAG, "Unable to deallocate point | at min (1)");
                connection_status_lb.setText("Unable to deallocate point | at min (1)");
                return;
            }
            points += 1;
            points_lb.setText(pts + points);
        }
        else if (plus) connection_status_lb.setText("No more points left to allocate.");
        else if (!plus) connection_status_lb.setText("Unable to deallocate point | at min (1)");
    }

    // Connect button was pressed. Will begin connection thread and connect to server.
    private void connectToServer() {
        Log.d(TAG, "Connecting to server...");

        // Checking if already connected
        if (connected) {
            Log.d(TAG, "Already connected/connecting to server");
            connection_status_lb.setText("Already connected/connecting to server. | " + connection_status);
            return;
        }

        connected = true;
        connection_status = "Connecting to server...";
        connection_status_lb.setText(connection_status);

        Connection connection = new Connection();
        Thread thread = new Thread((connection));
        thread.start();
    }

    // ================================================
    // Networking Methods
    // ================================================

    private class Connection implements Runnable {
        // Tag
        String TAG = "Connection Thread";

        // Network Items
        String input;
        String[] inputs;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;

        // Starting network
        public void run() {
            Log.d(TAG, "Connection thread started");

            // Connecting to server
            if (!attemptConnection()) {
                Log.d(TAG, "Connection Failed.");
                connection_status = "Connection Failed.";
                updateConnectionStatus(connection_status);
                connected = false;
                return;
            }

            // Success
            Log.d(TAG, "Connection Succeeded");
            connection_status = "Connected to Server.";
            updateConnectionStatus(connection_status);

            // Init Streams
            Log.d(TAG, "Setting up Streams");
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to setup input and output streams");
                System.exit(1001);
            }
            Log.d(TAG, "Finished streams");

            // Setting up
            if (!setup()) {
                Log.d(TAG, "Setup Failed | " + connection_status);
                updateConnectionStatus("Setup Failed | " + connection_status);
                closeConnection();
                connected = false;
                return;
            }

            // Success | Playing
            Log.d(TAG, "Setup Succeeded");
            connection_status = "Playing...";
            updateConnectionStatus(connection_status);
            switchView();

            // Playing game
            Log.d(TAG, "Playing game");
            while(true) {
                if(!getStatus()) break;
                if(!sendAction()) break;
            }

            // Game finished | Either died or sever shutdown.
            switchView();
            connection_status = "";
            Log.d(TAG, "Connection Closed | Gameover.");
            updateConnectionStatus("Connection Closed | Gameover.");
            closeConnection();
            connected = false;
        }

        // Attempts to connect to the server
        private boolean attemptConnection() {
            Log.d(TAG, "Attempting connection...");

            // Checking if connection was already made
            if (socket != null) return true;

            // Attempting to connect
            try {
                // Getting server address and opening socket
                InetAddress serverAddr = InetAddress.getByName(host.getText().toString());
                socket = new Socket(serverAddr, Integer.parseInt(port.getText().toString()));
                return true;
            } catch (Exception e) {
                // Couldn't connect
                socket = null;
                return false;
            }
        }

        // Closes the connection
        private void closeConnection() {
            Log.d(TAG, "Closing Connection...");
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException ex) {
                Log.wtf(TAG, "Failed to close???");
            } finally {
                socket = null;
                in = null;
                out = null;
                resetPlay();
            }
            Log.d(TAG, "Finished");
        }

        // Setting up with server
        private boolean setup() {
            Log.d(TAG, "Setting up...");
            connection_status = "Connected to Server. | Setting up...";
            updateConnectionStatus(connection_status);

            try {
                // Getting game information
                input = in.readLine();
                inputs = input.split(" ");
                Log.d(TAG, "Message: " + input);
                PID = Integer.parseInt(inputs[1]);
                arena_width = Integer.parseInt(inputs[2]);
                arean_height = Integer.parseInt(inputs[3]);
                numBots = Integer.parseInt(inputs[4]);
                team = Integer.parseInt(inputs[5]);

                // Sending bot setup info to server
                input = name + " " + (armor_pts - 1) + " " + (power_pts - 1) + " " + (scan_pts - 1);
                Log.d(TAG, "Sending: " + input);
                out.println(input);

                // Getting bot info
                input = in.readLine();
                inputs = input.split(" ");
                Log.d(TAG, "Message: " + input);

                // Checking if setup error
                // Shouldn't happen, but it's nice to know about.
                if(inputs[1].equalsIgnoreCase("error")) {
                    Log.d(TAG, "Invalid setup information.");
                    connection_status = "Invalid setup information.";
                    return false;
                }

                armor_value = Integer.parseInt(inputs[1]);
                moveRate = Integer.parseInt(inputs[2]);
                scanDistance = Integer.parseInt(inputs[3]);
                bulletPower = Integer.parseInt(inputs[4]);
                rateOfFire = Integer.parseInt(inputs[5]);
                bulletDistance = Integer.parseInt(inputs[6]);
                red = Integer.parseInt(inputs[7]);
                green = Integer.parseInt(inputs[8]);
                blue = Integer.parseInt(inputs[9]);

                // Adding bot to team (me)
                updateTeamColors(PID, team, red, green, blue);

                return true;
            } catch (Exception e) {
                connection_status = "Server Shutdown.";
                return false;
            }
        }

        // Gets a server status message from the server
        private boolean getStatus() {
            Log.d(TAG, "Getting server status");
            try {
                do {
                    input = in.readLine();
                    Log.d(TAG, "Message: " + input);
                    inputs = input.split(" ");

                    // Checking if info message
                    if (inputs[0].equalsIgnoreCase("info")) {
                        // Checking if gameover
                        if(!handleInfo()) return false;
                    }
                } while(inputs[0].equalsIgnoreCase("info"));

                // Got server status
                xPos = Integer.parseInt(inputs[1]);
                yPos = Integer.parseInt(inputs[2]);
                moveCount = Integer.parseInt(inputs[3]);
                shotCount = Integer.parseInt(inputs[4]);
                HP = Integer.parseInt(inputs[5]);
                updateBotStatus();

                return true;
            } catch (IOException e) {
                Log.d(TAG, "Server Shutdown.");
                return false;
            }
        }

        // Sends an action to the server
        private boolean sendAction() {
            Log.d(TAG, "Sending action to server.");

            // Bot shoots at powerup, uses an action to scan.
            if (action_shootPowerup && shotCount == 0) {
                Log.d(TAG, "Shooting Powerup");
                // Scanning
                if (!powerup_found) {
                    Log.d(TAG, "Sending: scan");
                    out.println("scan");
                    if (!handleScan()) return false;
                    if (!powerup_found) action_shootPowerup = false;
                    return true;
                }

                // Fireing at powerup if within scan (may not hit due to bullet range)
                Log.d(TAG, "Fireing at nearby powerup.");
                fire_angle = getFireAngle(powerup_xPos, powerup_yPos);
                input = "fire " + fire_angle;
                Log.d(TAG, "Sending: " + input);
                out.println(input);
                action_shootPowerup = false;
                powerup_found = false;
            }
            // Bot fireing
            else if (action_fire && shotCount == 0) {
                // Fireing
                input = "fire " + fire_angle;
                Log.d(TAG, "Sending: " + input);
                out.println(input);
                action_fire = false;
            }
            // Moving bot
            // Movement: XY
            // -1-1 | 0-1 | 1-1
            // -10  | 00  | 10
            // -11  | 01  | 11
            else if (action_move && moveCount == 0 && messages_till_scan > 0) {
                input = "move " + moveX + " " + moveY;
                Log.d(TAG, "Sending: " + input);
                out.println(input);
                action_move = false;
            }
            // Scanning
            else if (action_move || action_fire || messages_till_scan <= 0) {
                Log.d(TAG, "Sending: scan");
                out.println("scan");
                if(!handleScan()) return false;
            }
            // Nooping
            else {
                Log.d(TAG, "Sending: noop");
                out.println("noop");
            }
            messages_till_scan -= 1;
            return true;
        }

        // Handles info data being sent from the server.
        private boolean handleInfo() {
            Log.d(TAG, "Handleing Info Message");
            if (inputs[1].equalsIgnoreCase("hit")) updatePlayStatus("You've been hit!");
            else if (inputs[1].equalsIgnoreCase("badcmd")) updatePlayStatus("Error: Invalid Command");
            else if (inputs[1].equalsIgnoreCase("dead")) return false;
            else if (inputs[1].equalsIgnoreCase("gameover")) return false;
            else if (inputs[1].equalsIgnoreCase("alive")) numBots = Integer.parseInt(inputs[2]);
            else if (inputs[1].equalsIgnoreCase("powerup")) {
                String powerup = inputs[2];
                if (powerup.equalsIgnoreCase("armorup")) updatePlayStatus("Collected Powerup | HP+ (Max 5)");
                else if (powerup.equalsIgnoreCase("movefaster")) updatePlayStatus("Collected Powerup | Speed+");
                else if (powerup.equalsIgnoreCase("firefaster")) updatePlayStatus("Collected Powerup | ROF+");
                else if (powerup.equalsIgnoreCase("fireup")) updatePlayStatus("Collected Powerup | DMG+ | Bullet dist-");
                else if (powerup.equalsIgnoreCase("firemovefaster")) updatePlayStatus("Collected Powerup | Bullet spd+");
                else if (powerup.equalsIgnoreCase("teleport")) updatePlayStatus("Collected Powerup | Teleported");
            }
            return true;
        }

        // Handles scan data being sent from the server.
        private boolean handleScan() {
            Log.d(TAG, "Handleing Scan Message(s)");

            // Init
            messages_till_scan = 10;
            powerup_found = false;
            try {
                do {
                    input = in.readLine();
                    Log.d(TAG, "Message: " + input);
                    inputs = input.split(" ");

                    if (inputs[1].equalsIgnoreCase("bot")) {
                        Log.d(TAG, "Found Bot");
                        updateTeamColors(Integer.parseInt(inputs[2]),
                                Integer.parseInt(inputs[5]), Integer.parseInt(inputs[6]),
                                Integer.parseInt(inputs[7]), Integer.parseInt(inputs[8]));
                    }
                    else if (inputs[1].equalsIgnoreCase("shot")) Log.d(TAG, "Found Shot");
                    else if (inputs[1].equalsIgnoreCase("powerup")) {
                        Log.d(TAG, "Found Powerup");
                        int powerup = Integer.parseInt(inputs[2]);
                        if (powerup == 0) updatePlayStatus("HP+ (Max 5)");
                        else if (powerup == 1) updatePowerUpStatus("Speed+");
                        else if (powerup == 2) updatePowerUpStatus("ROF+");
                        else if (powerup == 3) updatePowerUpStatus("Bot AOE");
                        else if (powerup == 4) updatePowerUpStatus("Bomb");
                        else if (powerup == 5) updatePowerUpStatus("DMG+ | Bullet dist-");
                        else if (powerup == 6) updatePowerUpStatus("Bullet spd+");
                        else if (powerup == 7) updatePowerUpStatus("Teleport");
                        powerup_found = true;
                        powerup_xPos = Integer.parseInt(inputs[3]);
                        powerup_yPos = Integer.parseInt(inputs[4]);
                    }
                } while (!inputs[1].equalsIgnoreCase("done"));

                // Checking if powerup was found
                if (!powerup_found) updatePowerUpStatus("Not in scan range.");

                return true;
            } catch (IOException e) {
                Log.d(TAG, "Sever Shutdown");
                return false;
            }
        }
    }
}
