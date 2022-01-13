/*
@File: BluetoothConnectionThread.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 03
@Due: March 25, 2020
@Description: ---
 */

package com.robertrandolph.tictactoe_connected;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class BluetoothConnectionThread extends Thread {

    // TAG
    private static final String TAG = "BluetoothServerThread";

    // Consts
    private static final TicTacToeFragment.Venue SERVER = TicTacToeFragment.Venue.SERVER;
    private static final TicTacToeFragment.Venue CLIENT = TicTacToeFragment.Venue.CLIENT;

    // Thread Status
    // WAITING for connected device
    // SENDING from current device
    // CLOSING thread
    enum threadStatus {WAITING, SENDING, CLOSING, GAMEOVER}

    // Threading
    private final Object lock = new Object();

    // Listener
    private NetworkListener listener;

    // IO
    private BufferedReader in;
    private PrintWriter out;
    private String value;

    // Values
    private TicTacToeFragment.Venue venue;
    private boolean playAsX;
    private threadStatus status;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket clientSocket;

    // Constructor - Server
    BluetoothConnectionThread(BluetoothAdapter bluetoothAdapter, NetworkListener listener, boolean playAsX) {
        Log.d(TAG, "Init Server Thread");
        venue = SERVER;
        this.listener = listener;
        this.playAsX = playAsX;
        serverSocket = null;
        clientSocket = null;

        Log.d(TAG, "Starting server");
        try {
            serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(MainActivity.NAME, MainActivity.MYUUID);
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to start server");
        }
    }

    // Constructor - Client
    BluetoothConnectionThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice, NetworkListener listener) {
        Log.d(TAG, "Init Client thread");
        venue = CLIENT;
        this.bluetoothAdapter = bluetoothAdapter;
        this.listener = listener;
        clientSocket = null;

        Log.d(TAG, "Starting client");
        try {
            clientSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MainActivity.MYUUID);
            bluetoothAdapter.cancelDiscovery();
        } catch (IOException e) {
            Log.wtf(TAG, "Failed to start client");
            bluetoothAdapter.startDiscovery();
        }
    }

    // Running thread
    public void run() {
        Log.d(TAG, "Running Bluetooth Thread...");
        try {
            // Attempting to make connection
            // Server
            if (venue == SERVER) {
                Log.d(TAG, "Server: Accepting Client");
                clientSocket = serverSocket.accept();
            }
            // Client
            else {
                Log.d(TAG, "Client: Connecting to server");
                clientSocket.connect();
            }

            // Connection good
            Log.d(TAG, "Connection succeeded");
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            // Exchanging player info
            if (venue == SERVER) {
                if (!exchangePlayerInfoServer()) {
                    return;
                }
            }
            else if (!exchangePlayerInfoClient()) {
                return;
            }

            // Updating thread status
            if (playAsX) status = threadStatus.SENDING;
            else status = threadStatus.WAITING;

            // Notifying listener that connection is ready, and can be interacted with by the user
            if (listener != null) listener.connectionReady(playAsX);

            // Running game
            runGame();

        } catch(Exception e) {
            Log.wtf(TAG, "Failed read write");
        } finally {
            try {
                // Closing open connections
                Log.d(TAG, "Closing connections");
                listener.connectionClosed();
                if (venue == CLIENT) bluetoothAdapter.startDiscovery();
                if (venue == SERVER) serverSocket.close();
                clientSocket.close();
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.wtf(TAG, "Failed to close socket, Reader, or Writer");
            }
        }
        Log.d(TAG, "Finished running");
    }

    // Runs the initial player setup for the server
    // Returns true if player info was exchanged successfully, and false otherwise
    private boolean exchangePlayerInfoServer() throws Exception {
        Log.d(TAG, "Server: Initial player setup");

        // Sending player info
        Log.d(TAG, "Sending: player " + ((playAsX) ? "X" : "O"));
        out.println("player " + ((playAsX) ? "X" : "O"));
        out.flush();

        // Receiving and validating response
        value = in.readLine();
        Log.d(TAG, "Received: " + value);
        if(!validateResponse()) {
            Log.wtf(TAG, "Client failed to agree with player info");
            return false;
        }
        Log.d(TAG, "Server: Finished initial player setup");

        return true;
    }

    // Runs the initial player setup for the client
    // Returns true if player info was exchanged successfully, and false otherwise
    private boolean exchangePlayerInfoClient() throws Exception {
        Log.d(TAG, "Client: initial player setup");

        // Receiving player info
        value = in.readLine();
        Log.d(TAG, "Received: " + value);

        // Validating player info
        // If invalid sends negative response and closes connection
        if (value.equalsIgnoreCase("player X")) playAsX = false;
        else if (value.equalsIgnoreCase("player O")) playAsX = true;
        else {
            Log.wtf(TAG, "Server failed to provide valid player info");
            closeGame("disagree");
            return false;
        }

        // Sending response
        out.println("agree");
        out.flush();
        Log.d(TAG, "Client: Finished initial player setup");

        return true;
    }

    // Runs the game
    private void runGame() {
        Log.d(TAG, "Running game loop");
        while (true) {
            synchronized (lock) {
                if (status == threadStatus.WAITING) {
                    try {
                        if (in.ready()) {
                            receiveMove();
                        }
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed ready/write");
                        closeGame("exit");
                    }
                }
                // Thread is closing
                else if (status == threadStatus.CLOSING) {
                    Log.d(TAG, "Closing connection");
                    break;
                }
            }
        }
    }

    // Closes the thread
    public void closeGame(String send) {
        Log.d(TAG, "Closing game");
        if (!send.isEmpty()) {
            try {
                Log.d(TAG, "Sending: " + send);
                out.println(send);
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to read/write");
            }
        }
        synchronized (lock) {
            status = threadStatus.CLOSING;
        }
    }

    // Sends a move from the current device to the connected device
    public void sendMove(int move, TicTacToeTextureView.GameState gameState) {
        Log.d(TAG, "Sending move to connected device");

        synchronized (lock) {
            try {
                // Checking if thread is in sending status
                if (status == threadStatus.WAITING) {
                    Log.d(TAG, "Didn't send move, waiting for connected device to take action");
                    return;
                }
                else if (status == threadStatus.CLOSING) {
                    Log.d(TAG, "Didn't send move, thread is closing");
                    return;
                }
                else if (status == threadStatus.GAMEOVER) {
                    Log.d(TAG, "Didn't send move, game is over");
                    return;
                }

                // Sending move
                Log.d(TAG, "Sending: " + move);
                out.println(move);
                out.flush();

                // Receiving and validating response
                value = in.readLine();
                Log.d(TAG, "Received: " + value);
                if (!validateResponse()) {
                    Log.wtf(TAG, "Connected device failed to agree with move");
                    return;
                }

                // Sending game state
                if (gameState == TicTacToeTextureView.GameState.PLAYING) value = "nowinner";
                else if (gameState == TicTacToeTextureView.GameState.TIE) value = "tie";
                else value = "winner";
                Log.d(TAG, "Sending: " + value);
                out.println(value);
                out.flush();

                // Receiving and validating response
                value = in.readLine();
                Log.d(TAG, "Received: " + value);
                if (!validateResponse()) {
                    Log.wtf(TAG, "Connected device failed to agree with gamestate");
                    return;
                }

                // Good to go, updating status
                // Game is over
                if (gameState != TicTacToeTextureView.GameState.PLAYING) {
                    status = threadStatus.GAMEOVER;
                }
                // Game is in play
                else {
                    status = threadStatus.WAITING;
                }
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to read/write");
                closeGame("exit");
            }
        }
    }

    // Handles the response from the other player
    private void receiveMove() {
        // Init
        Log.d(TAG, "Receiving action");
        int move;

        try {
            // Getting player move
            value = in.readLine();
            Log.d(TAG, "Received: " + value);

            // Checking if integer
            // If not, closes game
            try {
                move = Integer.parseInt(value);
            } catch (Exception e) {
                Log.wtf(TAG, "Didn't receive move | (Not an integer)");
                closeGame("exit");
                return;
            }

            // Validating player move
            // Move was invalid
            if (!listener.moveReceived(move)) {
                closeGame("disagree");
            }
            // Move was valid
            else {
                Log.d(TAG, "Sending: agree");
                out.println("agree");
                out.flush();
            }

            // Getting gamestate
            TicTacToeTextureView.GameState gameState = listener.getGameState();
            value = in.readLine();
            Log.d(TAG, "Received: " + value);

            // Validating gamestate
            // game state is valid
            if ((gameState == TicTacToeTextureView.GameState.PLAYING && value.equalsIgnoreCase("nowinner"))
                    || (gameState == TicTacToeTextureView.GameState.TIE && value.equalsIgnoreCase("tie"))
                    || (gameState == TicTacToeTextureView.GameState.WIN && value.equalsIgnoreCase("winner"))) {
                Log.d(TAG, "Sending: agree");
                out.println("agree");
                out.flush();

                // Checking if game is over
                if (gameState != TicTacToeTextureView.GameState.PLAYING) {
                    status = threadStatus.GAMEOVER;
                }
                // Game is still in play
                else {
                    status = threadStatus.SENDING;
                }
            }
            // Game state is invalid
            else {
                closeGame("disagree");
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to read/write");
            closeGame("exit");
        }
    }

    // Sends to the connected device the intention of playing the game again or not
    public void startPlayAgainProtocol(boolean playAgain) {
        Log.d(TAG, "Sending Play Again Status");

        synchronized (lock) {
            try {
                // Checking if thread is in GAMEOVER status
                if (status != threadStatus.GAMEOVER) {
                    Log.d(TAG, "Didn't send play again request, game is still in play");
                    return;
                }

                // Server
                if (venue == SERVER) {
                    // Server wants to play again
                    if (playAgain) {
                        Log.d(TAG, "Server-Sending: playagain");
                        out.println("playagain");
                        out.flush();

                        // Getting and validating response
                        value = in.readLine();
                        Log.d(TAG, "Received: " + value);
                        if (!validateResponse()) {
                            return;
                        }

                        // Client wants to play again
                        if(!exchangePlayerInfoServer()) {
                            return;
                        }
                        status = threadStatus.SENDING;
                        listener.resetGame(playAsX);
                    }
                    // Server doesn't want to play again
                    else {
                        closeGame("exit");
                    }
                }
                // Client
                else {
                    // Getting response from server
                    value = in.readLine();
                    Log.d(TAG, "Received: " + value);

                    // Server wants to play again
                    if (value.equalsIgnoreCase("playagain")) {
                        // Client wants to play again
                        if (playAgain) {
                            Log.d(TAG, "Client-Sending: agree");
                            out.println("agree");
                            out.flush();
                            if (!exchangePlayerInfoClient()) {
                                return;
                            }
                            status = threadStatus.WAITING;
                            listener.resetGame(playAsX);
                        }
                        // Client doesn't want to play again
                        else {
                            closeGame("disagree");
                        }
                    }
                    // Server doesn't want to play again
                    else {
                        closeGame("agree");
                    }
                }
            } catch (Exception e) {
                Log.wtf(TAG, "Failed to read/write");
                closeGame("exit");
            }
        }
    }

    // Validates a response from the connected device
    // If it fails then prints the given error message and sends back the given response to the connected device.
    // Closes the game on a negative response.
    // Returns true if the response was valid, and false otherwise
    private boolean validateResponse() {
        Log.d(TAG, "Validating response");

        if (!value.equalsIgnoreCase("agree")) {
            Log.wtf(TAG, "Response was negative");
            closeGame("");
            return false;
        }

        return true;
    }
}
