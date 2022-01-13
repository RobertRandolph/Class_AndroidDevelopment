/*
@File: Adapter.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
Adapter for the recycler view.
Handles tap events for each item.
Handles updates for swipes, which are called by the swipehelper.
*/

package com.robertrandolph.expensetracking;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private final String TAG = "Adapter";

    private List<Expense> entries;      // List of items
    private int rowLayout;              // Layout of items
    private DatabaseViewModel viewModel; // db view model
    private ExpenseDatabase db;         // db
    AppCompatActivity activity;         // main activity

    // Constructor
    public Adapter(DatabaseViewModel viewModel, int rowLayout, AppCompatActivity activity, ExpenseDatabase db) {
        this.viewModel = viewModel;
        this.rowLayout = rowLayout;
        this.activity = activity;
        this.db = db;

        // Observing changes to database.
        // If database changes sets the recycler view data to that of the new expenses.
        this.viewModel.getExpenses().observe(activity, new Observer<List<Expense>>() {
            @Override
            public void onChanged(List<Expense> expenses) {
                Log.d(TAG, "Data has been changed.");
                entries = expenses;
                notifyDataSetChanged();
            }
        });
    }

    // Starts a dialog to update an item in the database.
    public void updateExpense(int position) {
        Log.d(TAG, "Update Dialog Started");
        // Retrieving entry
        final Expense entry = entries.get(position);

        // Running update dialog
        DialogFragment dialog = DialogFragment.newInstance(entry, new DialogFragment.dialogListener() {
            @Override
            public void dialogFinished(final Expense expense) {
                // Dialog finished (saved pressed)
                Log.d(TAG, "Update Expense");

                // Checking if dialog was successful
                if (expense == null) {
                    Log.d(TAG, "Failed update");
                    Toast.makeText(activity, "Name, category, amount and date must be filled.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Dialog successful, updating expense.
                Thread thread = new Thread() {
                    public void run() {
                        Log.d(TAG, "Updating...");
                        db.ExpenseDAO().update(expense);
                        Log.d(TAG, "Finished");
                    }
                };
                thread.start();
            }
        });
        dialog.show(activity.getFragmentManager(), "Update Dialog");
    }

    // Deletes the entry.
    public void deleteExpense(int position) {
        Log.d(TAG, "Deleting Expense");

        // Retrieving entry
        final Expense entry = entries.get(position);

        // Deleting from database.
        Thread thread = new Thread() {
            public void run() {
                Log.d(TAG, "Deleting...");
                db.ExpenseDAO().deleteExpense(entry.getId());
                Log.d(TAG, "Finished.");
            }
        };
        thread.start();
    }

    // Creates new view
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(rowLayout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        // Retrieving entry
        Expense entry = entries.get(position);

        // Setting data
        holder.name.setText(entry.getName());
        holder.category.setText(entry.getCategory());
        holder.amount.setText(String.format("$%.2f", entry.getAmount()));
        holder.date.setText(entry.getDate());
        holder.notes.setText(entry.getNotes());

        // Setting listener for tap events, allowing edits.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateExpense(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    // View holder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public TextView category;
        public TextView amount;
        public TextView date;
        public TextView notes;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            category = view.findViewById(R.id.category);
            amount = view.findViewById(R.id.amount);
            date = view.findViewById(R.id.date);
            notes = view.findViewById(R.id.notes);
        }
    }
}
