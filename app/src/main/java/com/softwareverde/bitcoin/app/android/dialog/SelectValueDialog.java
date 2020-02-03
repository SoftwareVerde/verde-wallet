package com.softwareverde.bitcoin.app.android.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.Util;

import java.util.HashMap;

public class SelectValueDialog<T> extends AppCompatDialogFragment {
    public interface Callback<T> {
        void run(T originalValue, T newValue);
    }

    protected Activity _activity;
    protected View _view;
    protected Callback<T> _callback = null;
    protected String _titleText = "";
    protected String _content = null;
    protected T _originalValue = null;
    protected List<T> _values = new MutableList<T>(0);
    protected String _selectButtonText = "Okay";
    protected String _cancelButtonText = "Cancel";

    public void setActivity(final Activity activity) {
        _activity = activity;
    }

    @Override
    @SuppressLint("RestrictedApi")
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(_activity, R.style.VerdeDialogTheme);

        final LayoutInflater inflater = _activity.getLayoutInflater();
        _view = inflater.inflate(R.layout.select_value_layout, null);
        builder.setView(_view);

        if (_titleText != null) {
            ((TextView) _view.findViewById(R.id.select_value_title)).setText(_titleText);
        }

        final TextView contentView = _view.findViewById(R.id.select_value_content);
        if (_content != null) {
            contentView.setText(_content);
            contentView.setVisibility(View.VISIBLE);
        }
        else {
            contentView.setVisibility(View.GONE);
        }

        final RadioGroup radioGroup = _view.findViewById(R.id.select_dialog_radio_group);
        radioGroup.removeAllViews();

        RadioButton defaultRadioButton = null;
        final HashMap<RadioButton, T> radioButtons = new HashMap<RadioButton, T>();
        for (final T value : _values) {
            final AppCompatRadioButton radioButton = new AppCompatRadioButton(_activity);
            radioButton.setPadding(10, 10, 10, 10);
            radioButton.setTextColor(Color.parseColor("#FFFFFF"));
            radioButton.setHighlightColor(Color.parseColor("#FFFFFF"));
            radioButton.setLines(1);

            radioButton.setSupportButtonTintList(ContextCompat.getColorStateList(_activity, R.color.white));

            final RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 10, 10, 10);
            radioButton.setLayoutParams(params);

            final String valueString = value.toString();
            radioButton.setText(valueString);

            if (valueString.length() > 32) {
                radioButton.setTextSize(10);
            }

            radioGroup.addView(radioButton);
            radioButtons.put(radioButton, value);

            if (Util.areEqual(_originalValue, value)) {
                defaultRadioButton = radioButton;
            }
        }

        if (defaultRadioButton != null) {
            defaultRadioButton.toggle();
        }

        builder.setPositiveButton(_selectButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                final Callback<T> callback = _callback;
                if (callback != null) {
                    for (final RadioButton radioButton : radioButtons.keySet()) {
                        final T value = radioButtons.get(radioButton);

                        if (radioButton.isChecked()) {
                            callback.run(_originalValue, value);
                            return;
                        }
                    }

                    callback.run(_originalValue, null);
                }
            }
        });

        builder.setNegativeButton(_cancelButtonText, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                SelectValueDialog.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    public void setTitle(final String title) {
        _titleText = title;
    }

    public void setValue(final T currentValue) {
        _originalValue = currentValue;
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

    public void setSelectButtonText(final String selectButtonText) {
        _selectButtonText = selectButtonText;
    }

    public void setCancelButtonText(final String cancelButtonText) {
        _cancelButtonText = cancelButtonText;
    }
}
