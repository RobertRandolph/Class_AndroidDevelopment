/*
@File: NetworkListener.java
@Author: Robert Randolph
@Class: COSC 5735-01
@Assignment: Program 03
@Due: March 25, 2020
@Description: ---
 */

package com.robertrandolph.tictactoe_connected;

public interface NetworkListener {
    void connectionReady(boolean playAsX);
    void connectionClosed();
    void playAgain();
    void exitGame();
    void resetGame(boolean playAsX);
    void moveMade(int move, TicTacToeTextureView.GameState gameState);
    boolean moveReceived(int move);
    TicTacToeTextureView.GameState getGameState();
}
