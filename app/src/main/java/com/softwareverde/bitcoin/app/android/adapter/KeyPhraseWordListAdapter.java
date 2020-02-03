package com.softwareverde.bitcoin.app.android.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.Util;

import java.util.Comparator;

public class KeyPhraseWordListAdapter extends RecyclerView.Adapter<KeyPhraseWordListAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(String word);
    }

    protected interface _OnClickListener {
        void onClick(int position);
    }

    protected Activity _activity;
    protected LayoutInflater _inflater;

    protected String _currentInput = "";

    protected List<String> _seedWords = new MutableList<String>(0);
    protected MutableList<String> _possibleMatches = new MutableList<String>();

    protected OnClickListener _onClickListener;

    protected final _OnClickListener __onClickListener = new _OnClickListener() {
        @Override
        public void onClick(final int position) {
            final OnClickListener onClickListener = _onClickListener;
            if (onClickListener == null) { return; }

            final String word = _possibleMatches.get(position);
            onClickListener.onClick(word);
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final TextView wordTextView;

        public ViewHolder(final View view) {
            super(view);

            this.view = view;
            this.wordTextView = view.findViewById(R.id.key_phrase_word);
        }
    }

    protected void _rebuildPossibleMatches() {
        _possibleMatches.clear();

        for (final String word : _seedWords) {
            if (word.startsWith(_currentInput)) {
                _possibleMatches.add(word);
            }
        }

        _sortDataSet();
    }

    private void _sortDataSet() {
        _possibleMatches.sort(new Comparator<String>() {
            @Override
            public int compare(final String word0, final String word1) {
                return (word0.compareTo(word1));
            }
        });
    }

    public KeyPhraseWordListAdapter(final Activity activity) {
        _activity = activity;
        _inflater = LayoutInflater.from(_activity.getApplicationContext());
    }

    public void setSeedWords(final List<String> seedWords) {
        _seedWords = seedWords;

        _rebuildPossibleMatches();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return _possibleMatches.getSize();
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        final String word = _possibleMatches.get(position);

        viewHolder.wordTextView.setText(word);
    }

    @Override
    public KeyPhraseWordListAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int position) {
        final View view = _inflater.inflate(R.layout.key_phrase_word_item, viewGroup, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final int position = viewHolder.getAdapterPosition();
                if (position < 0) { return; }

                __onClickListener.onClick(position);
            }
        });

        return viewHolder;
    }


    public String getItem(final int position) {
        if ( (position < 0) || (position >= _possibleMatches.getSize()) ) { return null; }

        return _possibleMatches.get(position);
    }

    public boolean isEmpty() {
        return (_possibleMatches.getSize() == 0);
    }

    public void setOnClickListener(final OnClickListener onClickListener) {
        _onClickListener = onClickListener;
    }

    public void onInputChanged(final String input) {
        final String lowerCaseInput = input.toLowerCase();
        if (Util.areEqual(_currentInput, lowerCaseInput)) { return; }
        _currentInput = lowerCaseInput;

        _rebuildPossibleMatches();
        this.notifyDataSetChanged();
    }

    public void clear() {
        _possibleMatches.clear();

        this.notifyDataSetChanged();
    }
}
