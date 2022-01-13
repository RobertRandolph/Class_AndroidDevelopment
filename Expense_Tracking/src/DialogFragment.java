/*
@File: DialogFragment.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 04
@Due: October 23, 2019
Handles user inputs for updating and inserting expenses in the database.
*/

package com.robertrandolph.expensetracking;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class DialogFragment extends android.app.DialogFragment {

    private static final String TAG = "Dialog";

    // Stored information
    private Expense expense;
    private dialogListener listener;

    // Constructor
    public DialogFragment() {} // Nothing

    // Creating new dialog.
    public static DialogFragment newInstance(Expense expense, dialogListener listener) {
        Log.d(TAG, "New dialog instance");
        // Init dialog info
        DialogFragment fragment = new DialogFragment();
        fragment.expense = expense;
        fragment.listener = listener;

        // Returning dialog
        return fragment;
    }

    // Init dialog
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "Init dialog");
        // Inflating
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog, null);

        // Getting edit texts for expense item.
        final EditText d_name = view.findViewById(R.id.d_name);
        final EditText d_category = view.findViewById(R.id.d_category);
        final EditText d_amount = view.findViewById(R.id.d_amount);
        final EditText d_date = view.findViewById(R.id.d_date);
        final EditText d_notes = view.findViewById(R.id.d_notes);

        // Assigning expense values to the dialog if it exists
        // That is, if updating an existing expense.
        if (expense != null) {
            Log.d(TAG, "Set expense");
            d_name.setText(expense.getName());
            d_category.setText(expense.getCategory());
            d_amount.setText(String.format("%.2f", expense.getAmount()));
            d_date.setText(expense.getDate());
            d_notes.setText(expense.getNotes());
        }

        // Building dialog
        Log.d(TAG, "Building Dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.ThemeOverlay_AppCompat_Dialog));
        builder.setView(view);

        // Checking if inserting an expense.
        if (expense == null) {
            Log.d(TAG, "Creating insert dialog");
            builder.setTitle("New Expense")
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Creating new expense.
                    // Checking user input
                    String name = d_name.getText().toString();
                    String category = d_category.getText().toString();
                    String amount = d_amount.getText().toString();
                    String date = d_date.getText().toString();

                    if (name.isEmpty() || category.isEmpty() || amount.isEmpty()|| date.isEmpty()) {
                        Log.d(TAG, "Missing information for new insert, canceling dialog");
                        listener.dialogFinished(null);
                        dialog.cancel();
                        return;
                    }

                    // Creating new expense
                    expense = new Expense(name, category, Double.parseDouble(amount), date, d_notes.getText().toString());

                    // Sending it to caller
                    listener.dialogFinished(expense);
                }
            });
        }

        // Checking if updating an expense
        else {
            Log.d(TAG, "Creating update dialog");
            builder.setTitle("Update Expense")
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Checking input
                    String name = d_name.getText().toString();
                    String category = d_category.getText().toString();
                    String amount = d_amount.getText().toString();
                    String date = d_date.getText().toString();

                    if (name.isEmpty() || category.isEmpty() || amount.isEmpty()|| date.isEmpty()) {
                        Log.d(TAG, "Missing information for expense update, canceling dialog");
                        listener.dialogFinished(null);
                        dialog.cancel();
                        return;
                    }

                    // Updating expense.
                    expense.setName(name);
                    expense.setCategory(category);
                    expense.setAmount(Double.parseDouble(amount));
                    expense.setDate(date);
                    expense.setNotes(d_notes.getText().toString());

                    // Sending it to caller.
                    listener.dialogFinished(expense);
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        Log.d(TAG, "Returning dialog");
        return builder.create();
    }

    // Interface listener for dialog.
    interface dialogListener {
        void dialogFinished(Expense expense);
    }
}
