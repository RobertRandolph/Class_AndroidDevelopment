/*
@File: ExpenseDAO.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
The DAO for the expense database.
*/

package com.robertrandolph.expensetracking;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDAO {

    // Selects all expenses
    @Query("SELECT * FROM " + Expense.TABLE_NAME)
    LiveData<List<Expense>> selectAll();

    // Inserts an expense into the database.
    // Returns the id of the entry.
    @Insert
    long insertExpense(Expense expense);

    // Updates the expense.
    // Returns number of rows updated.
    @Update
    int update(Expense expense);

    // Deletes the expense.
    // Returns number of deleted rows.
    @Query("DELETE FROM " + Expense.TABLE_NAME + " WHERE " + Expense.COLUMN_ID + " = :id")
    int deleteExpense(long id);
}
