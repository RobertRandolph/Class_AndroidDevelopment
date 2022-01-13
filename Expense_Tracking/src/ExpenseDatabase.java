/*
@File: ExpenseDatabase.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
The database for the app.
*/

package com.robertrandolph.expensetracking;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Expense.class}, version = 1, exportSchema = false)
public abstract  class ExpenseDatabase extends RoomDatabase {

    // Database name
    public static final String DATABASE_NAME = "expenseDatabase.db";

    // The database and DAO
    private static ExpenseDatabase db;
    public abstract ExpenseDAO ExpenseDAO();

    // Constructor, Returns the database
    // Creates it if not built
    public static ExpenseDatabase getInstance(final Context context) {
        if (db == null) {
            db = Room.databaseBuilder(context, ExpenseDatabase.class, DATABASE_NAME).build();
        }
        return db;
    }

}
