/*
@File: MainActivity.java
@Author: Robert Randolph
@Class: COSC 4730 - 01
@Assign: Program 03
@Due: October 2nd, 2019
Calculator.

Possible input: {[0-9] .}
Possible Operations: {+ - * / =}
Possible Actions: Clear(C), Clear Expression(CE), Delete(DEL), Negate(+-)

==Input==
User input is stored as an expression which will display when input.
After an operation the expression will reset and the total will display.
If no further input is given then further operations will use the total.
If any input is added (even if deleted later) it will use the user input.

After one expression is entered and an operation is pressed, the operation won't execute
until another expression is added (even if that is the total)

==Operations==
Pressing '=' more than once repeats the last operation.
After pressing equal if you press a separate operation
it will use the current total (now the expression)
and pressing equals again will repeat the operation using the total instead.

Pressing an operation, and then another right after will switch the operation. (not including =)

==Actions==
Clearing will reset the calculator to its initial state
Clearing an expression will clear the current user input. It leaves the total alone.
    Clearing an expression after the equals operations resets the calculator.
Deleting will only remove the last character of a user defined expression.
    This ignores totals.
Negating will flip the sign of a user defined expression or total depending on
    which one is displaying. This does not affect operations. (such as pressing = repeatedly)

==Display==
Displays current total, or user expression if given.
Displays the current sign of the total when visible, and expression when given.
Displays the current operation that the user has selected. (+, -, *, /)

==Examples==
each character {0-9.} is an input.
Only the specific number expressions in-between operations will display to the user.
if you see "(e) x" then it initially shows e until x is pressed, which then show x.

2 + 2 = 4 = 6 = 8 = 10 + = 20 = 30 + 45 del 4 = 34
2 + 2 - (4) 8 / (-4) 4 = -1
2 + 2 + (4) 2 + (6) 2 + (8) 22 CE (0) 2 = 10
2 + 2 = 4 C 0 2 + 2 = 4
3 / 0 = Cannot divide by zero
2 * 2 = 4 = 8 = 16
12.1 + 12.2 = 24.3
5 neg -5 + 5 = 0 + 5 neg -5 = -5 = -10
10 + 2 = 12 neg -12 = -10 = -8
3 + - 2 = 1 = -1 = -3
 */

package com.robertrandolph.calculator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";

    // Widgets
    private Button zero, one, two, three, four, five, six, seven, eight, nine;
    private Button  dot, plus, minus, mul, div, equal, negate, clearE, clear, del;
    private TextView expression, sign, operation;

    private String currentE, prevE;     // User defined expression | Previously used expression (may be total)
    private Action currentOP, prevOP;   // Selected operation | Previously used operation.
    private double total;               // The current total. (Holds sign)
    private boolean pos, prevPos;       // Current expression sign | Previous expression sign. || (Not for total)
    private boolean input;              // Input determines if an operation uses defined expression or total.
                                        // Keeping track if the user added input after an operation.

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // Getting widgets & setting listeners
        // Buttons
        zero = findViewById(R.id.zero);
        zero.setOnClickListener(this);
        one = findViewById(R.id.one);
        one.setOnClickListener(this);
        two = findViewById(R.id.two);
        two.setOnClickListener(this);
        three = findViewById(R.id.three);
        three.setOnClickListener(this);
        four = findViewById(R.id.four);
        four.setOnClickListener(this);
        five = findViewById(R.id.five);
        five.setOnClickListener(this);
        six = findViewById(R.id.six);
        six.setOnClickListener(this);
        seven = findViewById(R.id.seven);
        seven.setOnClickListener(this);
        eight = findViewById(R.id.eight);
        eight.setOnClickListener(this);
        nine = findViewById(R.id.nine);
        nine.setOnClickListener(this);
        plus = findViewById(R.id.plus);
        plus.setOnClickListener(this);
        minus = findViewById(R.id.minus);
        minus.setOnClickListener(this);
        mul = findViewById(R.id.mul);
        mul.setOnClickListener(this);
        div = findViewById(R.id.div);
        div.setOnClickListener(this);
        equal = findViewById(R.id.equal);
        equal.setOnClickListener(this);
        dot = findViewById(R.id.dot);
        dot.setOnClickListener(this);
        negate = findViewById(R.id.negate);
        negate.setOnClickListener(this);
        clearE = findViewById(R.id.clearE);
        clearE.setOnClickListener(this);
        clear = findViewById(R.id.clear);
        clear.setOnClickListener(this);
        del = findViewById(R.id.del);
        del.setOnClickListener(this);

        // Text View
        expression = findViewById(R.id.expression);
        sign = findViewById(R.id.sign);
        operation = findViewById(R.id.operation);

        // Init
        reset();
    }

    // Separates button events into their designated parts.
    public void onClick(View v) {
        Log.d(TAG, "Button pressed.");

        // Numbers -> expression
        if (v == zero) append("0");
        else if (v == one) append("1");
        else if (v == two) append("2");
        else if (v == three) append("3");
        else if (v == four) append("4");
        else if (v == five) append("5");
        else if (v == six) append("6");
        else if (v == seven) append("7");
        else if (v == eight) append("8");
        else if (v == nine) append("9");
        else if (v == dot) append(".");

        // Operations -> operates on expression
        else if (v == plus) operation(Action.ePlus);
        else if (v == minus) operation(Action.eMinus);
        else if (v == mul) operation(Action.eMul);
        else if (v == div) operation(Action.eDiv);
        else if (v == equal) operation(Action.eEqual);

        // Actions -> affects expression and calculator
        else if (v == negate) action(Action.eNegate);
        else if (v == clearE) action(Action.eClearE);
        else if (v == clear) action(Action.eClear);
        else action(Action.eDel);  // del
    }

    /*
     Appends the given character to the expression.
     If there is no expression, that is, if it's 0, and '0' is pressed
     the expression will not change.
     If there is already a dot . and another is appended
     the expression will not change.
     If the last operation was equals then it resets the calculator.
    */
    private void append(String a) {
        Log.d(TAG, "Value " + a + " pressed.");
        Log.d(TAG, "Current expression before: " + currentE);

        // Checking if last operation was equals.
        // If so, resets calculator.
        if (currentOP == Action.eEqual) reset();
        input = true;   // User input

        // Checking if the current expression isn't whats displayed.
        // (Happens when total is displayed instead)
        if (!expression.getText().toString().equals(currentE)) {
            expression.setText(currentE);
            updateSign();
        }

        // Checking if 0 was pressed with no expression.
        if (currentE.equals("0") && a.equals("0")) {
            Log.d(TAG, "0 pressed with no expression.");
            return; // skip
        }

        // Checking for more than one '.'
        if (currentE.contains(".") && a.equals(".")) {
            Log.d(TAG, "Expression already contains a dot .");
            return; // skip
        }

        // Preparing (new) expression for appending.
        if (currentE.equals("0") && !a.equals(".")) currentE = "";

        // Appending character to expression.
        currentE += a;

        // Assigning updated expression.
        Log.d(TAG, "Current expression after: " + currentE);
        expression.setText(currentE);
    }

    /*
     Handles basic operations. six (5) operations
     Plus, minus, multiply, divide, and equal.
     An operation occurs between the given expression and the total.
     If there wasn't a total at the start then it sets the total as the current expression
        on the first operation.
     The current operation is held and when a second expression is given it
     executes the operation.
     If another operation is selected without changing the expression the
     operation will switch.
     Pressing equals multiple times will repeat the previous operation.
     Pressing an operation and then equals without modifying the expression
     will use the total as the expression instead of user input (as it wasn't given)
     -----------------------------------------------------------
     @input below: determines if operation is executed using
     user defined expression when it's given
     or the current total when it's not.
    */
    private void operation(Action o) {
        double exp; // What will be used in an operation along with the total.
        Log.d(TAG, "Operation " + o.name() + " pressed.");
        Log.d(TAG, "Current expression before: " + currentE);
        Log.d(TAG, "Current total before: " + total);

        // Retrieving double from expression.
        // @input above
        if (!input && prevOP == null) exp = total;
        else {
            try {
                exp = Double.parseDouble(currentE);
                if (!pos) exp *= -1;    // Flipping sign if negative.
            } catch (NumberFormatException e) {
                Log.wtf(TAG, "Unable to turn expression into double.");
                return;
            }
        }

        if (currentOP != null) Log.d(TAG, "Calculating operation: " + currentOP.name());

        // Checking if there was an operation that needs executing.
        // If there wasn't simply sets the total.
        if (currentOP == null) total = exp;
        // Checking if operation switched (assigned below)
        else if (currentE.equals("0") && o != Action.eEqual) {} // Nothing
        // Executing selected operation.
        else if (currentOP == Action.ePlus) total += exp;
        else if (currentOP == Action.eMinus) total -= exp;
        else if (currentOP == Action.eMul) total *= exp;
        else if (currentOP == Action.eDiv) {
            // Checking if dividing by 0.
            if (exp == 0) {
                expression.setText("Cannot divide by zero");
                return;
            }
            total /= exp;
        }
        // Checking for equals
        // If equals was pressed previously it will repeat the last operation.
        else if (currentOP == Action.eEqual && o == Action.eEqual && prevOP != Action.eEqual && prevOP != null) {
            // Repeating the last operation.
            Log.d(TAG, "Repeating previous operation || exp: " + prevE + ", op: " + prevOP.name() + ", pos: " + prevPos);
            currentE = prevE;
            currentOP = prevOP;
            pos = prevPos;
            operation(currentOP);

            // Retrieving lost exp from repeated operation.
            exp = Double.parseDouble(prevE);
        }

        // Storing current operation if it was executed.
        // Only occurs if the first operation selected was not equal, and the second was.
        if (currentOP != null && currentOP != Action.eEqual && o == Action.eEqual) {
            // Holding current operation information.
            Log.d(TAG, "Storing current operation || exp: " + exp + ", op: " + currentOP.name() + ", pos: " + pos);
            if (exp < 0) exp *= -1;
            prevE = String.format("%s", exp);
            prevOP = currentOP;
            prevPos = pos;
        }
        else prevOP = null;

        // Holding the current operation.
        // Resets input and exp.
        currentOP = o;
        input = false;
        updateOperation();
        resetExp();

        // Displaying total.
        Log.d(TAG, "Current expression after: " + currentE);
        Log.d(TAG, "Current total after: " + total);
        displayTotal();
    }

    // Handles actions (4).
    // Negation will negate either the total or the current expression if given.
    // CE resets the current expression. If no expression was given resets calculator instead.
    // C resets the calculator.
    // Del removes the last character of a user defined expression.
    private void action(Action a) {
        Log.d(TAG, "Action " + a.name() + " pressed.");
        Log.d(TAG, "Current expression before: " + currentE);
        Log.d(TAG, "Current total before: " + total);

        // Going through possible actions.
        if (a == Action.eNegate) {
            // Checking if expression is empty.
            if (!currentE.equals("0")) {
                // Expression not empty.
                // Flipping sign of expression
                Log.d(TAG, "Negating expression.");
                pos = !pos;
                updateSign();
            }
            // Checking if total is 0. (Expression is empty)
            else if (total != 0) {
                // No expression, updating total.
                // Flipping total.
                Log.d(TAG, "Negating total.");
                total *= -1;
                displayTotal();
                return;
            }
        }
        else if (a == Action.eClearE) {
            // Checking if last operation was equals.
            if (currentOP == Action.eEqual) reset();
            else resetExp();
        }
        else if (a == Action.eClear) reset();
        else if (currentE.length() > 0) { // del
            // Checking if current expression is 0
            if (currentE.equals("0")) return; // Does nothing
            input = true;   // User input

            // Removing last character.
            currentE = currentE.substring(0, currentE.length() - 1);
            if (currentE.length() == 0) resetExp();

            // Updating expression
            expression.setText(currentE);
        }

        Log.d(TAG, "Current expression after: " + currentE);
        Log.d(TAG, "Current total after: " + total);
    }

    // Displays the total on the expression line.
    private void displayTotal() {
        Log.d(TAG, "Displaying total: " + total);
        double exp = total;
        if (exp < 0) {
            exp*= -1;
            sign.setText("-");
        }
        else sign.setText("");
        expression.setText(String.format("%s", exp));
    }

    // Update operation
    private void updateOperation() {
        Log.d(TAG, "Updating Operation: " + currentOP.name());
        if (currentOP == Action.ePlus) operation.setText("+");
        else if (currentOP == Action.eMinus) operation.setText("-");
        else if (currentOP == Action.eMul) operation.setText("*");
        else if (currentOP == Action.eDiv) operation.setText("/");
        else operation.setText("");
    }

    // Update sign
    private void updateSign() {
        Log.d(TAG, "Updating Sign: " + pos);
        if (pos) sign.setText("");
        else sign.setText("-");
    }

    // Resets the current expression.
    private void resetExp() {
        Log.d(TAG, "Resetting expression.");
        currentE = "0";
        pos = true;
        expression.setText(currentE);
        sign.setText("");
    }

    // Resets the calculator.
    private void reset() {
        Log.d(TAG, "Resetting Calculator.");
        resetExp();
        currentOP = null;
        prevOP = null;
        total = 0;
        input = false;
        operation.setText("");
    }
}

enum Action { ePlus, eMinus, eMul, eDiv, eEqual, eNegate, eClearE, eClear, eDel }