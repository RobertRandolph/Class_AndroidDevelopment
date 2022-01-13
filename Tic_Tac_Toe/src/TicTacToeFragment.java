/*
@File: TicTacToeFragment.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 03
@Due: March 25, 2020
@Description: ---
 */

package com.robertrandolph.tictactoe_connected;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class TicTacToeFragment extends Fragment implements View.OnClickListener, NetworkListener {

    // TAG
    private static final String TAG = "TicTacToeFragment";

    // Constants
    public enum Venue {SERVER, CLIENT}

    // Tic Tac Toe Board
    private TicTacToeTextureView board;

    // Widgets
    private ViewFlipper viewFlipper;
    private Button network_device, network_server, network_client;
    private RadioButton playAsX;
    private TextView help;

    // Bluetooth
    private BluetoothConnectionThread bluetoothConnectionThread;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;

    // Flags
    private Venue venue;
    private boolean active;

    // Values
    private String helpMessage;

    // Constructor
    public TicTacToeFragment() {}   // Ignored

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Inflating view");
        View view = inflater.inflate(R.layout.fragment_tic_tac_toe, container, false);

        // Setting up tic tac toe board
        board = new TicTacToeTextureView(getActivity(), this);
        FrameLayout fl = view.findViewById(R.id.board_container);
        fl.addView(board);

        // Setting up widgets
        viewFlipper = view.findViewById(R.id.viewFlipper);
        network_device = view.findViewById(R.id.network_device);
        network_device.setOnClickListener(this);
        network_server = view.findViewById(R.id.network_server);
        network_server.setOnClickListener(this);
        network_client = view.findViewById(R.id.network_client);
        network_client.setEnabled(false);
        network_client.setOnClickListener(this);
        playAsX = view.findViewById(R.id.playAsX);
        help = view.findViewById(R.id.help);

        // Init flags
        venue = null;
        active = false;

        // Init values
        helpMessage = "";

        // Setting up bluetooth
        bluetoothDevice = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        updateView();

        // Returning view
        return view;
    }

    // Switches between the connection and the board views
    private void switchView() {
        Log.d(TAG, "Switching View");
        new Handler(Objects.requireNonNull(getActivity()).getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                viewFlipper.showNext();
            }
        });
    }

    // Updates the network view
    @SuppressLint("SetTextI18n")
    private void updateView() {
        Log.d(TAG, "Updating view");
        new Handler(Objects.requireNonNull(getActivity()).getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
            // Device supports bluetooth
            if (bluetoothAdapter != null) {
                // Currently connected
                if (venue != null) {
                    network_device.setEnabled(false);
                    // Connected as server
                    if (venue == Venue.SERVER) {
                        network_server.setEnabled(false);
                        network_client.setEnabled(false);
                    }
                    // Connected as client
                    else if (venue == Venue.CLIENT) {
                        network_server.setEnabled(false);
                        network_client.setEnabled(false);
                    }
                }
                // Not connected
                else {
                    network_device.setEnabled(true);
                    network_server.setText(getString(R.string.server_start));
                    network_server.setEnabled(true);
                    if (bluetoothDevice == null) network_client.setEnabled(false);
                    else network_client.setEnabled(true);
                    network_client.setText(getString(R.string.client_start));
                }
                help.setText(getString(R.string.bluetooth_help) + helpMessage);
            }
            // Device doesn't support Bluetooth
            else {
                Log.d(TAG, "Device doesn't support bluetooth");
                network_device.setEnabled(false);
                network_client.setEnabled(false);
                network_server.setEnabled(false);
                help.setText(getString(R.string.bluetooth_notSupported));
            }
            }
        });
    }

    // Queries for paired bluetooth devices and starts a dialog to select one.
    private void queryPairedDevices() {
        Log.d(TAG, "Querying paired devices");

        // Init
        Log.d(TAG, "Getting paired bluetooth devices");
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> devices = new ArrayList<>();
        ArrayList<String> items = new ArrayList<>();

        // Checking if any devices were found
        // If not, returns
        if (pairedDevices.isEmpty()) {
            bluetoothDevice = null;
            network_client.setEnabled(false);
            help.append("No paired devices found\n");
            return;
        }

        // Consolidating device information
        Log.d(TAG, " Compiling paired device information");
        for (BluetoothDevice device : pairedDevices) {
            devices.add(device);
            items.add(device.getName() + ": " + device.getAddress());
        }

        // Creating dialog
        Log.d(TAG, "Building Dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Paired Bluetooth Device:")
                .setSingleChoiceItems(items.toArray(new String[0]), -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        bluetoothDevice = devices.get(which);
                        network_client.setEnabled(true);
                    }
                })
                .create().show();
    }

    //=====================================================\\
    // Network
    //=====================================================\\

    // Starts a server connection
    private void startServer() {
        Log.d(TAG, "Starting server");
        venue = Venue.SERVER;
        helpMessage = "Server started, waiting for client...\n";
        updateView();
        bluetoothConnectionThread = new BluetoothConnectionThread(bluetoothAdapter, this, playAsX.isChecked());
        bluetoothConnectionThread.start();
    }

    // Starts the client for the current connection
    private void startClient() {
        Log.d(TAG, "Starting client");
        venue = Venue.CLIENT;
        updateView();
        helpMessage = "Connecting to server...";
        bluetoothConnectionThread = new BluetoothConnectionThread(bluetoothAdapter, bluetoothDevice, this);
        bluetoothConnectionThread.start();
    }

    //=====================================================\\
    // Listener Call Backs
    //=====================================================\\

    // Handles button events
    // If the current device is running a client or server then it doesn't do anything
    // Paired devices will list all paired devices of the current device.
    // The user can select one to connect to and play the game.
    @Override
    public void onClick(View v) {
        Log.d(TAG, "Button Clicked");

        // Checking if server or client is running on current device
        // If so, simply returns
        if (venue != null) {
            return;
        }

        // Paired Devices
        if (v == network_device) {
            queryPairedDevices();
        }
        // Server
        else if (v == network_server) {
            startServer();
        }
        // Client
        else if (v == network_client) {
            startClient();
        }
    }

    // The bluetooth connection is ready and can be interacted with the user
    //
    @Override
    public synchronized void connectionReady(boolean playAsX) {
        Log.d(TAG, "Connection Ready");
        switchView();
        active = true;
        board.setGame(playAsX);
    }

    // The bluetooth connection closed and can no longer be interacted with by the user.
    @Override
    public synchronized void connectionClosed() {
        Log.d(TAG, "Connection closed");
        if (active) switchView();
        active = false;
        venue = null;
        bluetoothConnectionThread = null;
        helpMessage = "Connection closed.";
        updateView();
    }

    // Current user wants to play again
    // Notifies connected device
    @Override
    public synchronized void playAgain() {
        bluetoothConnectionThread.startPlayAgainProtocol(true);
    }

    // Game was exited by the user
    // Notifies connected device.
    @Override
    public synchronized void exitGame() {
        Log.d(TAG, "Exiting game");
        TicTacToeTextureView.GameState gameState = board.getGameState();

        // Game is over, notifying connected device if the current device wants to play again
        if (gameState != TicTacToeTextureView.GameState.PLAYING) {
            bluetoothConnectionThread.startPlayAgainProtocol(false);
        }
        // Game is still in play, closing the game
        else {
            bluetoothConnectionThread.closeGame("exit");
        }
    }

    // Tells the board to reset with the given player info
    @Override
    public void resetGame(boolean playAsX) {
        Log.d(TAG, "Resetting game");
        board.setGame(playAsX);
    }

    // Current device has made a valid move, sending move to connected device
    @Override
    public void moveMade(int move, TicTacToeTextureView.GameState gameState) {
        bluetoothConnectionThread.sendMove(move, gameState);
    }

    // Received move from connected device, attempting to do move.
    // Returns true if the move was successful, otherwise false
    @Override
    public boolean moveReceived(int move) {
        return board.setMove(move);
    }

    // Returns the game state to the thread
    @Override
    public TicTacToeTextureView.GameState getGameState() {
        return board.getGameState();
    }
}