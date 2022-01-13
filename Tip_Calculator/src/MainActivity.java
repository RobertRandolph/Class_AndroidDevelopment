/*
@File: MainActivity.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 02
@Due: September 23, 2019
Tip Calculator.
Takes the bill amount, and the tip percentage and calculates the tip amount.
Tip is rounded based on user preference.
Also has 3 quick tip buttons.
*/

package com.robertrandolph.tip_calculator;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {

    private static String TAG = "MainActivity";

    private EditText bill, tipPercent;
    private TextView tipAmount, totalBill;
    private RadioGroup method, type;
    private Button ten, fifteen, twenty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // Setting Components
        // EditTexts
        // What the user will edit
        bill = findViewById(R.id.billAmount);
        bill.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                // Checking if bill has more then 2 decimal places, or leading 0's.
                String b = s.toString();
                Log.d(TAG, "Bill Amount: " + b);

                // Checking if empty
                if (b.isEmpty()) {
                    calculateTip();
                    return;
                }

                // Checking for leading 0's
                if (b.startsWith("0")) {
                    bill.setText(b.substring(1));
                    bill.setSelection(bill.length());
                }

                // Looking for .
                if (b.contains(".")) {
                    String decimals = b.substring(b.indexOf("."));

                    // Checking decimal digit count.
                    if (decimals.length() > 3) {
                        Log.d(TAG, "Bill Amount has too many decimal digits.");
                        Log.d(TAG, "Removing last digit.");
                        bill.setText(b.substring(0, b.length()-1));
                        bill.setSelection(bill.length());
                    }
                    // Calculating tip
                    else calculateTip();
                }
                // Calculating tip
                else calculateTip();
            }
        });
        tipPercent = findViewById(R.id.tipPercent);
        tipPercent.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                // Checking if percent is over 100, or has leading 0's
                String p = s.toString();
                int percent;
                Log.d(TAG, "Tip Percent: " + tipPercent.getText());

                // Checking if empty
                if (p.isEmpty()) {
                    calculateTip();
                    return;
                }

                // Checking for leading 0's
                if (p.startsWith("0")) {
                    tipPercent.setText(p.substring(1));
                    tipPercent.setSelection(tipPercent.length());
                }

                // Getting tip percent.
                try {
                    percent = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    Log.wtf(TAG, "Couldn't convert percent to integer.");
                    return;
                }

                // Checking if percent is over 100
                if (percent > 100) {
                    Log.d(TAG, "Tip percent over 100");
                    Log.d(TAG, "Setting percent to 100.");
                    tipPercent.setText("100");
                    tipPercent.setSelection(tipPercent.length());
                }

                // Calculating tip
                calculateTip();
            }
        });

        // TextView
        // What the user will see
        tipAmount = findViewById(R.id.tipAmount);
        totalBill = findViewById(R.id.totalBill);

        // Radio Groups
        // Settings for the user
        method = findViewById(R.id.roundingMethod);
        method.setOnCheckedChangeListener(this);
        type = findViewById(R.id.roundDollar);
        type.setOnCheckedChangeListener(this);

        // Buttons
        // Quick tips
        ten = findViewById(R.id.tenPercent);
        ten.setOnClickListener(this);
        fifteen = findViewById(R.id.fifteenPercent);
        fifteen.setOnClickListener(this);
        twenty = findViewById(R.id.twentyPercent);
        twenty.setOnClickListener(this);
    }

    // Recalculates the tip and total bill if a rounding setting changed.
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Log.d(TAG, "Rounding method/type changed");
        calculateTip();
    }

    // Sets the tip percentage depending on which button was pressed.
    // Will show keyboard for bill amount to quickly enter bill.
    // Will recalculate the tip amount and total bill.
    public void onClick(View v) {
        Log.d(TAG, "Quick Tip pressed:");
        if (v == ten) {
            Log.d(TAG, "10 Percent");
            tipPercent.setText("10");
        }
        else if (v == fifteen) {
            Log.d(TAG, "15 Percent");
            tipPercent.setText("15");
        }
        else {  // twenty
            Log.d(TAG, "20 Percent");
            tipPercent.setText("20");
        }

        // Moving cursor to end of tip amount.
        tipPercent.setSelection(tipPercent.length());

        // Showing keyboard for bill amount.
        bill.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(bill, InputMethodManager.SHOW_IMPLICIT);
    }

    // Calculates the tip amount, and total bill with tip.
    public void calculateTip() {
        String b = bill.getText().toString();
        String p = tipPercent.getText().toString();
        double bill, tipPercent, tipAmount, temp = 0;
        String finalTipAmount, totalBill;
        int selected;
        boolean bill_flag = false;

        Log.d(TAG, "Calculating Tip");

        // Checking if bill or tip percent was empty.
        if (b.isEmpty() || p.isEmpty()) {
            Log.d(TAG, "Bill or Tip Percent was empty");
            this.tipAmount.setText("$0.00");
            this.totalBill.setText("$0.00");
            return;
        }

        // Getting bill and tip percentage
        try {
            bill = Double.parseDouble(b);
            tipPercent = Double.parseDouble(p);
            tipPercent /= 100;
            Log.d(TAG, "Tip percenage: " + tipPercent);
        } catch (NumberFormatException e) {
            Log.wtf(TAG, "Couldn't convert bill or tip percent to double.");
            return;
        }

        // Calculating initial tip.
        tipAmount = bill*tipPercent;
        Log.d(TAG, "Initial Tip: " + tipAmount);

        // Applying rounding method
        selected = method.getCheckedRadioButtonId();
        if (selected == R.id.noRounding) {
            Log.d(TAG, "Not rounding.");
        }
        else if (selected == R.id.roundTotalBill) {
            Log.d(TAG, "Rounding Total Bill");
            Log.d(TAG, "Current Bill: " + bill);
            bill_flag = true;

            // Calculating current total bill.
            temp = bill + tipAmount;
            Log.d(TAG, "Bill with tip: " + temp);
        }
        else { // Round tip
            Log.d(TAG, "Rounding tip");
            temp = tipAmount;
        }

        // Applying rounding type
        if (selected != R.id.noRounding) {
            selected = type.getCheckedRadioButtonId();
            if (selected == R.id.roundNearest) {
                Log.d(TAG, "Rounding to nearest dollar.");
                temp = Math.round(temp);
            } else if (selected == R.id.roundUp) {
                Log.d(TAG, "Rounding up");
                temp = Math.ceil(temp);
            } else { // Round Down
                Log.d(TAG, "Rounding Down");
                temp = Math.floor(temp);
            }

            // Calculating round-off
            if (bill_flag) temp -= bill;
            temp -= tipAmount;
            Log.d(TAG, "Tip round-off: " + temp);
        }

        // Getting final tip and bill amounts
        finalTipAmount = String.format("%.2f", tipAmount + temp);
        totalBill = String.format("%.2f", bill + tipAmount + temp);
        Log.d(TAG, "Final tip amount: " + finalTipAmount);
        Log.d(TAG, "Total bill: " + totalBill);
        this.tipAmount.setText("$" + finalTipAmount);
        this.totalBill.setText("$" + totalBill);
    }
}
