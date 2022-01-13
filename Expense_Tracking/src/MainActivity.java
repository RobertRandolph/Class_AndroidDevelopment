/*
@File: MainActivity.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
Handles the floating action button, which adds new entries into the database.
*/

package com.robertrandolph.expensetracking;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "MainActivity";

    // Database
    ExpenseDatabase db;
    Adapter adapter;

    // Widgets
    RecyclerView recyclerview;
    FloatingActionButton fab_add;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init database
        // Getting database and view model
        db = ExpenseDatabase.getInstance(this);
        DatabaseViewModel viewModel = new DatabaseViewModel(db);

        // Init widgets
        // Init recycler view
        recyclerview = findViewById(R.id.recycler);
        recyclerview.setHasFixedSize(true);
        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setItemAnimator(new DefaultItemAnimator());
        // Setting adapter & swipe helper
        adapter = new Adapter(viewModel, R.layout.expense_view, this, db);
        recyclerview.setAdapter(adapter);
        new ItemTouchHelper(new SwipeHelper(this, adapter)).attachToRecyclerView(recyclerview);

        // Init Floating Action Button
        fab_add = findViewById(R.id.fab_add);
        fab_add.setOnClickListener(this);
    }

    // Starts dialog to add an item.
    // Listening for FAB clicks.
    @Override
    public void onClick(View v) {
        Log.d(TAG, "FAB clicked");
        Log.d(TAG, "Insert Dialog started");

        // Running insert dialog
        DialogFragment dialog = DialogFragment.newInstance(null, new DialogFragment.dialogListener() {
            @Override
            public void dialogFinished(final Expense expense) {
                // Dialog finished (saved pressed)
                Log.d(TAG, "Inserting new expense.");

                // Checking if dialog finished successfully.
                if (expense == null) {
                    Log.d(TAG, "Expense was null");
                    Toast.makeText(MainActivity.this, "Name, category, amount and date must be filled.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Was successful. Adding expense to database.
                Thread thread = new Thread() {
                    public void run() {
                        Log.d(TAG, "Inserting...");
                        db.ExpenseDAO().insertExpense(expense);
                        Log.d(TAG, "Finished");
                    }
                };
                thread.start();
            }
        });
        dialog.show(getFragmentManager(), "Insert Dialog");
    }
}
