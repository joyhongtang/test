package com.idwell.cloudframe.widget;

import android.app.TimePickerDialog;
import android.content.Context;

public class MyTimePickerDialog extends TimePickerDialog {

    public MyTimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
        super(context, listener, hourOfDay, minute, is24HourView);
    }

    public MyTimePickerDialog(Context context, int themeResId, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
        super(context, themeResId, listener, hourOfDay, minute, is24HourView);
    }

    @Override
    protected void onStop() {
        //super.onStop();
    }
}
