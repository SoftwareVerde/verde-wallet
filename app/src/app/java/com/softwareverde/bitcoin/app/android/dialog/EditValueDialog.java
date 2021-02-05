package com.softwareverde.bitcoin.app.android.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;

public class EditValueDialog extends AppCompatDialogFragment {
    public interface Callback {
        void run(String originalValue, String newValue);
    }

    public enum Type {
        NUMERIC, STRING
    }

    protected Activity _activity;
    protected View _view;
    protected Callback _callback = null;
    protected String _titleText = "";
    protected String _content = null;
    protected String _originalValue = "";
    protected String _editButtonText = "Okay";
    protected String _cancelButtonText = "Cancel";
    protected Type _type = Type.STRING;

    public void setActivity(final Activity activity) {
        _activity = activity;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(_activity, R.style.VerdeDialogTheme);

        final LayoutInflater inflater = _activity.getLayoutInflater();
        _view = inflater.inflate(R.layout.edit_value_layout, null);
        builder.setView(_view);

        if (_titleText != null) {
            ((TextView) _view.findViewById(R.id.edit_value_title)).setText(_titleText);
        }

        final TextView contentView = _view.findViewById(R.id.edit_value_content);
        if (_content != null) {
            contentView.setText(_content);
            contentView.setVisibility(View.VISIBLE);
        }
        else {
            contentView.setVisibility(View.GONE);
        }

        final EditText editText = _view.findViewById(R.id.edit_value_input);
        if (_originalValue != null) {
            editText.setHint(_originalValue);
        }

        if (_type == Type.NUMERIC) {
            editText.setKeyListener(new DigitsKeyListener());
        }
        editText.setText(_originalValue);

        builder.setPositiveButton(_editButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (_callback != null) {
                    final String setValue = editText.getText().toString();
                    _callback.run(_originalValue, setValue);
                }
            }
        });

        builder.setNegativeButton(_cancelButtonText, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                EditValueDialog.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    public void setTitle(final String title) {
        _titleText = title;
    }

    public void setValue(final String currentValue) {
        _originalValue = currentValue;
    }

    public void setType(final Type type) {
        _type = type;
    }

    public void setContent(final String content) {
        _content = content;
    }

    public void setCallback(final Callback callback) {
        _callback = callback;
    }

    public void setEditButtonText(final String editButtonText) {
        _editButtonText = editButtonText;
    }

    public void setCancelButtonText(final String cancelButtonText) {
        _cancelButtonText = cancelButtonText;
    }
}
