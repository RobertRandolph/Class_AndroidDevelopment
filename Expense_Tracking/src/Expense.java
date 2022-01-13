/*
@File: Expense.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
A database entry for expenses.
Holds table and column information.
*/

package com.robertrandolph.expensetracking;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = Expense.TABLE_NAME)
public class Expense {

    // Table information
    public static final String TABLE_NAME = "expenses";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_NOTES = "notes";

    // Entry information
    // Entry unique id
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long id;
    // Name
    @ColumnInfo(name = COLUMN_NAME)
    public String name;
    // Category
    @ColumnInfo(name = COLUMN_CATEGORY)
    public String category;
    // Amount
    @ColumnInfo(name = COLUMN_AMOUNT)
    public double amount;
    // Date
    @ColumnInfo(name = COLUMN_DATE)
    public String date;
    // Notes (optional)
    @ColumnInfo(name = COLUMN_NOTES)
    public String notes;

    // Constructors
    @Ignore
    public Expense() {} // Nothing
    public Expense(String name, String category, double amount,
                   String date, String notes) {
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    // Getters and Setters
    // ID
    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    // Name
    public String getName() {return name;}
    public void setName(String name) {this.name = name; }
    // Category
    public String getCategory() {return category;}
    public void setCategory(String category) {this.category = category; }
    // Amount
    public double getAmount() {return amount;}
    public void setAmount(double amount) { this.amount = amount; }
    // Date
    public String getDate() { return date;}
    public void setDate(String date) { this.date = date; }
    // Notes
    public String getNotes() {return notes;}
    public void setNotes(String notes) { this.notes = notes; }
}
