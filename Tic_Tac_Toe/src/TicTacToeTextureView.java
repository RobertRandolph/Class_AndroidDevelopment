/*
@File: TicTacToeTextureView.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 03
@Due: March 25, 2020
@Description: ---
 */

package com.robertrandolph.tictactoe_connected;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Objects;

@SuppressLint("ViewConstructor")
public class TicTacToeTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    // TAG
    private static String TAG = "MyTextureView";

    // Gamestate
    public enum GameState {PLAYING, TIE, WIN}

    // Listener
    private NetworkListener listener;

    // Surface Params
    private float scale;                        // Scale of screen
    private float dHeight, dWidth, top;         // Surface Dimensions
    private float pDim, pOffset;                // Play Area Dimensions & offset
    private float pLeft, pRight, pTop, pBottom; // Play Area Boarders
    private final Rect eBounds;                 // Exit Button Bounds
    private final Rect mBounds;                 // Move Bounds

    // Paint
    private final Paint bg, gLine;  // Background and lines
    private final Paint hText;      // Header Text
    private final Paint mText;      // Move text

    // Current Game
    private ArrayList<String> board; // Moves the players have made
    private int numMoves;            // Number of turns made so far
    private String playerTurn;       // Which players turn it is
    private GameState gameState;     // State of the game (-1:playing | 0:tie | 1:win)
    private boolean playAsX;         // Whether the player on the current device is X

    // Constructor
    public TicTacToeTextureView(Context context, NetworkListener listener) {
        super(context);
        Log.d(TAG, "Constructor");

        // Init
        this.listener = listener;

        // Game
        board = new ArrayList<>(9);
        for(int i = 0; i < 9; i++) {
            board.add("");
        }

        // Surface Params
        eBounds = new Rect();
        mBounds = new Rect();
        scale = getResources().getDisplayMetrics().density;

        // Paint
        bg = new Paint();
        bg.setColor(Color.WHITE);

        gLine = new Paint();
        gLine.setColor(Color.rgb(211, 211, 211));
        gLine.setStrokeWidth(scale*5);

        hText = new Paint();
        hText.setStyle(Paint.Style.FILL);
        hText.setTextSize(20*scale);
        hText.setUnderlineText(true);
        hText.getTextBounds("Exit Game", 0, "Exit Game".length(), eBounds);

        mText = new Paint();
        mText.setStyle(Paint.Style.FILL);
        mText.setTextSize(50*scale);
        mText.getTextBounds("X", 0, "X".length(), mBounds);

        // Surface
        setSurfaceTextureListener(this);
    }

    //=====================================================\\
    // Game
    //=====================================================\\

    // Surface Events
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "Surface Available");
        // Init
        // Surface Params
        // Surface Dimensions
        dHeight = height;
        dWidth = width;
        top = dHeight*.2f;

        // Play Area Dimensions
        // Ensures that it fits on the screen
        pDim = dWidth*.8f;
        if (pDim > dHeight*.6f) pDim = dHeight*.6f;

        // Play Area Offsets
        pOffset = pDim/3;

        // Play Area Boarders
        pLeft = (dWidth - pDim)/2f;
        pRight = pDim + pLeft;
        pTop = dHeight*.3f;
        pBottom = pDim + pTop;

        // Drawing board
        drawBoard();
    }
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "Surface Size Changed");
    }
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "Surface Destroyed");
        return true;
    }
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "Surface Updated");
    }

    // Takes care of when the user touches the screen.
    // The screen is split into critical areas of interest
    // 0 is the Exit button
    // 1-9 is the play area
    // 10 is the Play Again area
    // -1 is anything we don't care about
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        // Checking action
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        Log.d(TAG, "Surface Touched");

        // Getting touch area
        int area = getTouchArea(event);
        Log.d(TAG, "Area: " + area + " touched");

        // Determine action based on area
        // Exit game
        if (area == 0) {
            resetGame();
            drawBoard();
            listener.exitGame();
        }
        // Play again if Win or Tie
        else if (area == 10 && gameState != GameState.PLAYING) {
            resetGame();
            drawBoard();
            listener.playAgain();
        }
        // Making player move
        else if (area > 0 && area < 10) {
            // Checking if current devices turn
            // It not, simply returns
            if (!(playAsX && playerTurn.equalsIgnoreCase("X"))
                    && (!(!playAsX && playerTurn.equalsIgnoreCase("O")))) {
                Log.d(TAG, "Not current devices turn");
            }
            else {
                if (setMove(area)) {
                    listener.moveMade(area, gameState);
                }
            }
        }

        // Consuming event
        return true;
    }

    // Determines the area of the user tap.
    // -1 : No critical area
    // 0 : Exit
    // 1-9 : Play Area
    // 10 : Play Again
    // =========
    // Play Area areas
    // 7 8 9
    // 4 5 6
    // 1 2 3
    private int getTouchArea(MotionEvent event) {
        Log.d(TAG, "Getting touch area");
        // Init
        int area = -1;
        float x = event.getX();
        float y = event.getY();

        // Checking if on Play Again Button
        if (x > 0 && x < dWidth*.5f && y < dHeight*.2f) {
            area = 10;
        }

        // Checking if on Exit button
        if (x > dWidth*.5f && x < dWidth && y < dHeight*.2f) {
            area = 0;
        }

        // Checking if in play area
        if (x > pLeft && x < pRight && y > pTop && y < pBottom) {
            // Calculating play area zones
            // Init
            float lx = pLeft;
            float rx = pLeft + pOffset;
            float ty = pTop;
            float tb = pTop + pOffset;

            // Going through zones
            for (int i = 0; i < 9; i++) {
                // Checking zone
                if (x > lx && x < rx && y > ty && y < tb) area = i + 1;

                // Going to next zone
                if (i%3 == 2) {
                    lx = pLeft;
                    rx = pLeft + pOffset;
                    ty += pOffset;
                    tb += pOffset;
                }
                else {
                    lx += pOffset;
                    rx += pOffset;
                }
            }
        }

        // Returning the converted touch area
        // Converted touch area is to translate between board coordinates and network coordinates.
        return convertTouchArea(area);
    }

    // Sets the move at the current area
    // Returns true if the move was valid, and false otherwise
    // @param area: Assumes entering value is network board area format
    public boolean setMove(int move) {
        Log.d(TAG, "Setting move: " + move);

        // Converting network area to board area
        move = convertTouchArea(move);

        // Checking if game is in play
        if (gameState != GameState.PLAYING) {
            Log.d(TAG, "Game isn't in play");
            return false;
        }

        // Validating area
        // Ensures that the given area is in the play zone and that the given area is empty.
        if (move < 1 || move > 9 || !board.get(move - 1).isEmpty()) {
            Log.d(TAG, "Invalid area");
            return false;
        }

        // Setting move
        Log.d(TAG, "Setting player move: " + playerTurn);
        board.set(move - 1, playerTurn);
        numMoves++;

        // Checking game state and Drawing board
        // Updating turn if game isn't over
        updateGameState(move);
        if (gameState == GameState.PLAYING) {
            if (playerTurn.equals("X")) playerTurn = "O";
            else playerTurn = "X";
        }
        drawBoard();
        return true;
    }

    // Converts the board area to the network board area
    // 7 8 9      1 2 3
    // 4 5 6  =>  4 5 6
    // 1 2 3      7 8 9
    private int convertTouchArea(int area) {
        Log.d(TAG, "Converting touch area from: " + area);
        switch (area) {
            case 1: area = 7; break;
            case 2: area = 8; break;
            case 3: area = 9; break;
            case 7: area = 1; break;
            case 8: area = 2; break;
            case 9: area = 3; break;
        }
        Log.d(TAG, "to: " + area);

        return area;
    }

    // Checks the game state to determine if game is over
    // Only checks the current players moves
    private void updateGameState(int area) {
        Log.d(TAG, "Checking Game State");

        // Checking number of moves
        if (numMoves < 5) return;

        // Init
        String move;
        area -= 1;
        int row = area/3;
        int col = area%3;

        // Looking through board
        // Checking row
        for (int r = 0; true; r++) {
            move = board.get(row*3 + r);            // Getting move
            if (!move.equals(playerTurn)) break;    // Checking if row move is not current player | yes, no row victory
            else if (r == 2) {                      // Checking if all row moves have been checked | yes, player won
                gameState = GameState.WIN;
                return;
            }
        }

        // Checking col
        for (int c = 0; true; c++) {
            move = board.get(c*3 + col);            // Getting move
            if (!move.equals(playerTurn)) break;    // Checking if col move is not current player | yes, no col victory
            else if (c == 2) {                      // Checking if all col moves have been checked | yes, player won
                gameState = GameState.WIN;
                return;
            }
        }

        // Checking diagonals
        // tl -> br
        for (int d = 0; true; d++) {
            move = board.get(d*3 + d);              // Getting move
            if (!move.equals(playerTurn)) break;    // Checking if dig move is not current player | yes, no col victory
            else if (d == 2) {                      // Checking if all dig moves have been checked | yes, player won
                gameState = GameState.WIN;
                return;
            }
        }
        // tr -> bl
        for (int d = 0; true; d++) {
            move = board.get(d*3 + 2 - d);          // Getting move
            if (!move.equals(playerTurn)) break;    // Checking if dig move is not current player | yes, no col victory
            else if (d == 2) {                      // Checking if all dig moves have been checked | yes, player won
                gameState = GameState.WIN;
                return;
            }
        }

        // Checking if tie
        if (numMoves == 9) {
            gameState = GameState.TIE;
        }
    }

    // Returns the game state of the given connection
    public GameState getGameState() {
        Log.d(TAG, "Returning gamestate");
        return gameState;
    }

    // Sets the current device player
    public void setGame(boolean playAsX) {
        Log.d(TAG, "Setting game");
        this.playAsX = playAsX;
        gameState = GameState.PLAYING;
        resetGame();
    }

    // Resets the game for the current connection
    private void resetGame() {
        Log.d(TAG, "Resetting game");
        for(int i = 0; i < 9; i++) board.set(i, "");
        numMoves = 0;
        playerTurn = "X";
        gameState = GameState.PLAYING;
    }

    // Draws Board
    private void drawBoard() {
        Log.d(TAG, "Drawing Board");

        // Init
        Rect bounds = new Rect();

        // Getting Canvas
        final Canvas canvas = lockCanvas(null);

        // Drawing background
        Log.d(TAG, "Drawing Background");
        canvas.drawRect(0, 0, dWidth, dHeight, bg);
        canvas.drawLine(0, top, dWidth, top, gLine);

        // Drawing Board Play Area
        Log.d(TAG, "Drawing Play Area");
        canvas.drawLine(pLeft + pOffset, pTop, pLeft + pOffset, pBottom, gLine);
        canvas.drawLine(pLeft + pOffset*2, pTop, pLeft + pOffset*2, pBottom, gLine);
        canvas.drawLine(pLeft, pTop + pOffset, pRight, pTop + pOffset, gLine);
        canvas.drawLine(pLeft, pTop + pOffset*2, pRight, pTop + pOffset*2, gLine);

        // Drawing Text
        Log.d(TAG, "Drawing Text");
        // Top Buttons
        if (gameState == GameState.WIN || gameState == GameState.TIE) {
            canvas.drawText("Play Again", dWidth*.1f, dHeight*.1f, hText);
        }
        else {
            canvas.drawText("Player Turn: " + playerTurn, dWidth*.1f, dHeight*.1f, hText);
        }
        canvas.drawText("Exit Game", dWidth*.9f - eBounds.width(), dHeight*.1f, hText);
        // Status
        // Game is in play
        if (gameState == GameState.PLAYING) {
            // Current devices turn
            if (playAsX && playerTurn.equalsIgnoreCase("X")) {
                hText.getTextBounds("Your Turn!", 0, "Your Turn!".length(), bounds);
                canvas.drawText("Your Turn!", dWidth*.5f - bounds.width()/2f, dHeight*.95f, hText);
            }
            // Connected devices turn
            else {
                hText.getTextBounds("Other Players Turn!", 0, "Other Players Turn!".length(), bounds);
                canvas.drawText("Other Players Turn!", dWidth*.5f - bounds.width()/2f, dHeight*.95f, hText);
            }
        }
        // Game is Won or Tied
        else {
            // Game is won
            if (gameState == GameState.WIN) {
                // Current devices turn
                if ((playAsX && playerTurn.equalsIgnoreCase("X"))
                || (!playAsX && playerTurn.equalsIgnoreCase("O"))) {
                    hText.getTextBounds("You Won!", 0, "You Won!".length(), bounds);
                    canvas.drawText("You Won!", dWidth*.5f - bounds.width()/2f, dHeight*.95f, hText);
                }
                // Connected devices turn
                else {
                    hText.getTextBounds("You Lost!", 0, "You Lost!".length(), bounds);
                    canvas.drawText("You Lost!", dWidth*.5f - bounds.width()/2f, dHeight*.95f, hText);
                }
            }
            // Game is a tie
            else {
                hText.getTextBounds("GameOver: Tie!", 0, "GameOver: Tie!".length(), bounds);
                canvas.drawText("GameOver: Tie!", dWidth*.5f - bounds.width()/2f, dHeight*.95f, hText);
            }
        }


        // Drawing Player Moves
        Log.d(TAG, "Drawing Moves");
        float x = pLeft + pOffset/2 - mBounds.width()/2f;
        float y = pTop + pOffset/2 + mBounds.height()/2f;
        for (int i = 0; i < 9; i++) {
            // Drawing Text
            canvas.drawText(board.get(i), x, y, mText);

            // Updating Coordinates.
            if (i%3 == 2) {
                x = pLeft + pOffset/2 - mBounds.width()/2f;
                y += pOffset;
            }
            else x += pOffset;
        }

        // Finished Drawing
        unlockCanvasAndPost(canvas);
        Log.d(TAG, "Drawing Finished");
    }
}
