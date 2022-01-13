/*
@File: DatabaseViewModel.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
view model for the data base
Places an observer on the database itself.
*/

package com.robertrandolph.expensetracking;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class DatabaseViewModel extends ViewModel {

    private final MediatorLiveData<List<Expense>> observableExpenses;

    public DatabaseViewModel(ExpenseDatabase db) {
        // Init observable
        observableExpenses = new MediatorLiveData<>();
        observableExpenses.setValue(null);

        // Getting expenses from database
        LiveData<List<Expense>> expenses = db.ExpenseDAO().selectAll();

        // Observe changes from db
        observableExpenses.addSource(expenses, new Observer<List<Expense>>() {
            @Override
            public void onChanged(List<Expense> expenses) {
                observableExpenses.setValue(expenses);
            }
        });
    }

    public LiveData<List<Expense>> getExpenses() {
        return observableExpenses;
    }
}
