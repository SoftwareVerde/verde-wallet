package com.softwareverde.bitcoin.app.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwareverde.android.util.FragmentUtil;
import com.softwareverde.bitcoin.app.MainActivity;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.BitcoinUtil;
import com.softwareverde.bitcoin.app.android.adapter.KeyPhraseWordListAdapter;
import com.softwareverde.bitcoin.app.lib.BitcoinVerde;
import com.softwareverde.bitcoin.secp256k1.key.PrivateKey;
import com.softwareverde.bitcoin.wallet.SeedPhraseGenerator;
import com.softwareverde.constable.bytearray.ByteArray;

public class ImportPrivateKeyFragment extends VerdeFragment {
    public interface ImportPrivateKeyCallback {
        void onPrivateKeyImported(PrivateKey privateKey);
    }

    protected static final String TAG = DisplayReceiveAddressFragment.class.getSimpleName();

    protected IBinder _windowToken = null;
    protected View _view;
    protected BitcoinVerde _bitcoinVerde;
    protected ImportPrivateKeyCallback _importPrivateKeyCallback;
    protected KeyPhraseWordListAdapter _keyPhraseWordListAdapter;
    protected Boolean _isInHexInputMode = false;

    protected void _deleteHexCharacter() {
        final TextView privateKeyTextView = _view.findViewById(R.id.import_private_key_phrase);
        final String newPrivateKeyString = privateKeyTextView.getText().toString().replaceAll("[^0-9A-Fa-f]", "");

        if (newPrivateKeyString.isEmpty()) {
            privateKeyTextView.setText("");
        }
        else {
            final String trimmedString = newPrivateKeyString.substring(0, newPrivateKeyString.length() - 1);
            privateKeyTextView.setText(_spaceHex(trimmedString));
        }
    }

    protected void _deletePhraseWord() {
        final TextView phraseTextView = _view.findViewById(R.id.import_private_key_phrase);
        final String phrase = phraseTextView.getText().toString();

        final String[] words = phrase.split(" ");

        String separator = "";
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < words.length; ++i) {
            final String word = words[i];
            if (i != (words.length - 1)) {
                stringBuilder.append(separator);
                stringBuilder.append(word);
            }

            separator = " ";
        }
        phraseTextView.setText(stringBuilder.toString());
    }

    protected String _spaceHex(final String hexString) {
        return BitcoinUtil.spaceHex(hexString, 4, 1);
    }

    protected void _hideKeyboard() {
        if (_view != null) {
            final EditText importPrivateKeyInput = _view.findViewById(R.id.import_private_key_input);
            importPrivateKeyInput.clearFocus();
        }

        final Activity activity = this.getActivity();
        if ( (activity != null) && (_windowToken != null) ) {
            final InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(_windowToken, 0);
            }
        }
    }

    protected void _updateView() {
        final TextView titleTextView = _view.findViewById(R.id.import_private_key_title);
        final View phraseWordList = _view.findViewById(R.id.import_private_key_phrase_word_list);
        final EditText importPrivateKeyInput = _view.findViewById(R.id.import_private_key_input);
        final ViewGroup inputContainer = _view.findViewById(R.id.import_private_key_phrase_hex_input_container);

        if (_isInHexInputMode) {
            titleTextView.setText("Import Private Key");
            phraseWordList.setVisibility(View.GONE);
            inputContainer.setVisibility(View.VISIBLE);
            importPrivateKeyInput.setVisibility(View.GONE);
        }
        else {
            titleTextView.setText("Import Private Key Phrase");
            phraseWordList.setVisibility(View.VISIBLE);
            inputContainer.setVisibility(View.GONE);
            importPrivateKeyInput.setVisibility(View.VISIBLE);
            importPrivateKeyInput.requestFocus();
        }
    }

    public ImportPrivateKeyFragment() { }

    public void setImportPrivateKeyCallback(final ImportPrivateKeyCallback importPrivateKeyCallback) {
        _importPrivateKeyCallback = importPrivateKeyCallback;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.import_private_key_layout, container, false);

        _keyPhraseWordListAdapter = new KeyPhraseWordListAdapter(this.getActivity());
        final TextView privateKeyPhraseTextView = _view.findViewById(R.id.import_private_key_phrase);

        { // Create hex keyboard...
            final ViewGroup inputContainer = _view.findViewById(R.id.import_private_key_phrase_hex_input_container);

            // B C D E F
            // 6 7 8 9 A
            // 1 2 3 4 5
            // 0

            LinearLayout buttonRowContainer = null;
            final int buttonsPerRow = 5;
            for (final char c : new char[]{ 'B', 'C', 'D', 'E', 'F', '6', '7', '8', '9', 'A', '1', '2', '3', '4', '5', '0' }) {
                if (buttonRowContainer == null) {
                    buttonRowContainer = new LinearLayout(this.getContext());
                    buttonRowContainer.setOrientation(LinearLayout.HORIZONTAL);
                    buttonRowContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.2F));
                }

                final TextView buttonTextView = (TextView) inflater.inflate(R.layout.keypad_button, buttonRowContainer, false);
                buttonTextView.setText(String.valueOf(c));
                buttonTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        final String newPrivateKeyString = (privateKeyPhraseTextView.getText().toString() + c).replaceAll("[^0-9A-Fa-f]", "");
                        privateKeyPhraseTextView.setText(_spaceHex(newPrivateKeyString));

                        if (newPrivateKeyString.length() == (PrivateKey.KEY_BYTE_COUNT * 2)) {
                            final PrivateKey privateKey = PrivateKey.fromBytes(ByteArray.fromHexString(newPrivateKeyString));
                            if (privateKey != null) {
                                _bitcoinVerde.addPrivateKey(privateKey);

                                { // Set the synchronization fragment on start-up...
                                    final FragmentActivity activity = ImportPrivateKeyFragment.this.getActivity();
                                    final FragmentManager fragmentManager = activity.getSupportFragmentManager();
                                    FragmentUtil.popFragment(fragmentManager);
                                }
                            }
                        }
                    }
                });
                buttonRowContainer.addView(buttonTextView);

                if (buttonRowContainer.getChildCount() == buttonsPerRow) {
                    inputContainer.addView(buttonRowContainer);
                    buttonRowContainer = null;
                }
            }
            if (buttonRowContainer != null) {
                inputContainer.addView(buttonRowContainer);
            }
        }

        final EditText importPrivateKeyInput = _view.findViewById(R.id.import_private_key_input);

        final RecyclerView keyPhraseWordListView = _view.findViewById(R.id.import_private_key_phrase_word_list);
        // keyPhraseWordListView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        keyPhraseWordListView.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        keyPhraseWordListView.setAdapter(_keyPhraseWordListAdapter);
        _keyPhraseWordListAdapter.setOnClickListener(new KeyPhraseWordListAdapter.OnClickListener() {
            @Override
            public void onClick(final String word) {
                final String phrase = privateKeyPhraseTextView.getText().toString();

                final String newPhrase;
                {
                    final StringBuilder newPhraseBuilder = new StringBuilder();
                    if (! phrase.isEmpty()) {
                        newPhraseBuilder.append(phrase);
                        newPhraseBuilder.append(" ");
                    }
                    newPhraseBuilder.append(word);
                    newPhrase = newPhraseBuilder.toString();
                }

                privateKeyPhraseTextView.setText(newPhrase);
                importPrivateKeyInput.setText("");

                final SeedPhraseGenerator seedPhraseGenerator = _bitcoinVerde.getSeedPhraseGenerator();
                final boolean seedPhraseIsValid = seedPhraseGenerator.isSeedPhraseValid(newPhrase);
                if (seedPhraseIsValid) {
                    final ByteArray byteArray = seedPhraseGenerator.fromSeedPhrase(newPhrase);
                    final PrivateKey privateKey = PrivateKey.fromBytes(byteArray);
                    if (privateKey != null) {
                        _bitcoinVerde.addPrivateKey(privateKey);

                        { // Set the synchronization fragment on start-up...
                            final FragmentActivity activity = ImportPrivateKeyFragment.this.getActivity();
                            final FragmentManager fragmentManager = activity.getSupportFragmentManager();
                            FragmentUtil.popFragment(fragmentManager);
                        }
                    }
                }
            }
        });

        _view.findViewById(R.id.import_private_key_phrase_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (_isInHexInputMode) {
                    _deleteHexCharacter();
                }
                else {
                    _deletePhraseWord();
                }
            }
        });

        _updateView();

        importPrivateKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int start, final int count, final int after) { }

            @Override
            public void onTextChanged(final CharSequence charSequence, final int start, final int before, final int count) {
                final String value = importPrivateKeyInput.getText().toString();
                _keyPhraseWordListAdapter.onInputChanged(value);
            }

            @Override
            public void afterTextChanged(final Editable editable) { }
        });

        importPrivateKeyInput.setFilters(
            new InputFilter[] {
                new InputFilter() {
                    final CharSequence REJECT = "";
                    final CharSequence ALLOW = null;

                    @Override
                    public CharSequence filter(final CharSequence source, final int start, final int end, final Spanned dest, final int dstart, final int dend) {
                        if (_isInHexInputMode) { return ALLOW; }

                        if ( (source != null) && (source.length() > 0) ) {
                            if (source.toString().matches("[^A-Za-z ]")) {
                                return REJECT;
                            }
                        }

                        return ALLOW;
                    }
                }
            }
        );

        _view.findViewById(R.id.import_private_key_hex_mode_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                _isInHexInputMode = (! _isInHexInputMode);

                importPrivateKeyInput.setText("");
                privateKeyPhraseTextView.setText("");

                if (_isInHexInputMode) {
                    _hideKeyboard();
                }

                _updateView();
            }
        });

        return _view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        final Activity activity = this.getActivity();
        if (activity != null) {
            _windowToken = activity.getWindow().getDecorView().getWindowToken();
        }

        _view.post(new Runnable() {
            @Override
            public void run() {
                if (activity instanceof MainActivity) {
                    final MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.requestFullLayout();
                }
            }
        });

        { // Show the keyboard...
            final EditText importPrivateKeyInput = _view.findViewById(R.id.import_private_key_input);

            importPrivateKeyInput.requestFocus();
            final InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(importPrivateKeyInput, InputMethodManager.SHOW_FORCED);
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        final Activity activity = this.getActivity();
        if (activity instanceof MainActivity) {
            final MainActivity mainActivity = (MainActivity) activity;
            mainActivity.requestNormalLayout();
        }

        _hideKeyboard();

        super.onPause();
    }

    @Override
    public void onDetach() {
        _hideKeyboard();

        super.onDetach();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();

        _keyPhraseWordListAdapter.setSeedWords(_bitcoinVerde.getSeedWords());

        _updateView();
    }

    @Override
    public void onServiceDisconnected() {
        _bitcoinVerde = null;
    }
}
