package com.softwareverde.bitcoin.app.android.adapter;

import com.softwareverde.bitcoin.wallet.slp.SlpToken;
import com.softwareverde.bitcoin.wallet.utxo.SpendableTransactionOutput;
import com.softwareverde.util.Util;

import java.util.Comparator;

public class SortAlgorithms {
    public static final Comparator<SpendableTransactionOutput> NONE = null;

    public static final Comparator<SpendableTransactionOutput> AMOUNT_ASCENDING = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.NONE, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> AMOUNT_DESCENDING = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.NONE, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> AMOUNT_ASCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.UNSPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> AMOUNT_DESCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.UNSPENT_FIRST, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> AMOUNT_ASCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.SPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> AMOUNT_DESCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.NONE, SpentMode.SPENT_FIRST, AmountMode.DESCENDING);

    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_ASCENDING = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.NONE, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_DESCENDING = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.NONE, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_ASCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.UNSPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_DESCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.UNSPENT_FIRST, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_ASCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.SPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_FIRST_AMOUNT_DESCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_FIRST, SpentMode.SPENT_FIRST, AmountMode.DESCENDING);

    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_ASCENDING = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.NONE, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_DESCENDING = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.NONE, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_ASCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.UNSPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_DESCENDING_UNSPENT_BEFORE_SPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.UNSPENT_FIRST, AmountMode.DESCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_ASCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.SPENT_FIRST, AmountMode.ASCENDING);
    public static final Comparator<SpendableTransactionOutput> TOKEN_LAST_AMOUNT_DESCENDING_SPENT_BEFORE_UNSPENT = new SpendableTransactionOutputComparator(TokenMode.TOKEN_LAST, SpentMode.SPENT_FIRST, AmountMode.DESCENDING);
}

enum TokenMode {
    NONE, TOKEN_FIRST, TOKEN_LAST
}

enum SpentMode {
    NONE, UNSPENT_FIRST, SPENT_FIRST
}

enum AmountMode {
    NONE, ASCENDING, DESCENDING
}

class SpendableTransactionOutputComparator implements Comparator<SpendableTransactionOutput> {

    protected final TokenMode _tokenMode;
    protected final SpentMode _spentMode;
    protected final AmountMode _amountMode;

    public SpendableTransactionOutputComparator(final TokenMode tokenMode, final SpentMode spentMode, final AmountMode amountMode) {
        _tokenMode = tokenMode;
        _spentMode = spentMode;
        _amountMode = amountMode;
    }

    @Override
    public int compare(final SpendableTransactionOutput spendableTransactionOutput0, final SpendableTransactionOutput spendableTransactionOutput1) {
        if (_tokenMode != TokenMode.NONE) {
            final Boolean outputIsSlp0 = (spendableTransactionOutput0 instanceof SlpToken);
            final Boolean outputIsSlp1 = (spendableTransactionOutput1 instanceof SlpToken);

            if (! Util.areEqual(outputIsSlp0, outputIsSlp1)) {
                return ( (_tokenMode == TokenMode.TOKEN_FIRST) ? outputIsSlp1.compareTo(outputIsSlp0) : outputIsSlp0.compareTo(outputIsSlp1) );
            }
        }

        if (_spentMode != SpentMode.NONE) {
            final Boolean outputIsSpent0 = spendableTransactionOutput0.isSpent();
            final Boolean outputIsSpent1 = spendableTransactionOutput1.isSpent();

            if (! Util.areEqual(outputIsSpent0, outputIsSpent1)) {
                return ( (_spentMode == SpentMode.UNSPENT_FIRST) ? outputIsSpent0.compareTo(outputIsSpent1) : outputIsSpent1.compareTo(outputIsSpent0) );
            }
        }

        if (_amountMode == AmountMode.NONE) { return 0; }

        final Long amount0 = spendableTransactionOutput0.getTransactionOutput().getAmount();
        final Long amount1 = spendableTransactionOutput1.getTransactionOutput().getAmount();
        return ( (_amountMode == AmountMode.ASCENDING) ? amount0.compareTo(amount1) : amount1.compareTo(amount0) );
    }
}