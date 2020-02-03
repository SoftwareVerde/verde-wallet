package com.softwareverde.bitcoin.app.android.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;

import java.util.HashMap;

public class ButtonDialog<T> extends AppCompatDialogFragment {
    public interface Callback<T> {
        void run(T selectedItem);
    }

    protected Activity _activity;
    protected View _view;
    protected Callback<T> _callback = null;
    protected String _titleText = "";
    protected String _content = null;
    protected List<T> _values = new MutableList<T>(0);
    protected String _cancelButtonText = "Cancel";

    public void setActivity(final Activity activity) {
        _activity = activity;
    }

    @Override
    @SuppressLint("RestrictedApi")
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(_activity, R.style.VerdeDialogTheme);

        final LayoutInflater inflater = _activity.getLayoutInflater();
        _view = inflater.inflate(R.layout.button_dialog_layout, null);
        builder.setView(_view);

        if (_titleText != null) {
            ((TextView) _view.findViewById(R.id.button_dialog_title)).setText(_titleText);
        }

        final TextView contentView = _view.findViewById(R.id.button_dialog_content);
        if (_content != null) {
            contentView.setText(_content);
            contentView.setVisibility(View.VISIBLE);
        }
        else {
            contentView.setVisibility(View.GONE);
        }

        final LinearLayout buttonContainer = _view.findViewById(R.id.button_dialog_items);
        buttonContainer.removeAllViews();

        final HashMap<View, T> buttons = new HashMap<View, T>();
        for (final T value : _values) {
            final AppCompatTextView button = new AppCompatTextView(_activity);
            button.setPadding(10, 10, 10, 10);
            button.setTextColor(Color.parseColor("#FFFFFF"));
            button.setBackgroundColor(Color.parseColor("#404040"));
            button.setTypeface(null, Typeface.BOLD);
            button.setTextSize(24);
            button.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            button.setLines(1);

            // Android Bug: RestrictedApi
            button.setAutoSizeTextTypeUniformWithConfiguration(8, 42, 1, TypedValue.COMPLEX_UNIT_SP);

            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(24, 24, 24, 24);
            button.setLayoutParams(params);

            final String valueString = value.toString();
            button.setText(valueString);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Callback<T> callback = _callback;
                    ButtonDialog.this.getDialog().cancel();

                    if (callback != null) {
                        callback.run(value);
                    }
                }
            });


            buttonContainer.addView(button);
            buttons.put(button, value);
        }

        builder.setPositiveButton(null, null);

        builder.setNegativeButton(_cancelButtonText, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                ButtonDialog.this.getDialog().cancel();
            }
        });

        // final AlertDialog alertDialog = builder.create();
        return builder.create();
    }

    public void setTitle(final String title) {
        _titleText = title;
    }

    public void setContent(final String content) {
        _content = content;
    }

    public void setCallback(final Callback<T> callback) {
        _callback = callback;
    }

    public void setValues(final List<T> values) {
        _values = values;
    }

    public void setCancelButtonText(final String cancelButtonText) {
        _cancelButtonText = cancelButtonText;
    }
}
