package com.softwareverde.app;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.secp256k1.key.PrivateKey;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.HexUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();

        final AddressInflater addressInflater = new AddressInflater();
        final TransactionInflater transactionInflater = new TransactionInflater();

        final Wallet wallet = new Wallet();

        int i = 0;
        final MutableList<Transaction> transactions = new MutableList<Transaction>();
        {
            // final InputStream inputStream = appContext.getResources().openRawResource(R.raw.transactions);
            final InputStream inputStream = null;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String byteString;
            while ((byteString = reader.readLine()) != null) {
                System.out.println(i++);
                final Transaction transaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray(byteString));
                if (transaction == null) {
                    System.out.println("Invalid Tx: " + byteString);
                    continue;
                }

                transactions.add(transaction);
            }
        }

        final MutableList<Address> addresses = new MutableList<Address>();
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("19784ED2D6D8ABD959B55B682828510413B3F51023B2E28CA7E9556A065CE664");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("BFE70A783DB6F0BEB821D9FACE85CCA9F997E6ABC77A17AE199A37AA3C2767D5");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("01D16F62F788E9AF42048A55952DCA9C308D5A0D81236EA3B06970370DC1E5E8");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("0AB2ACBE1D368DD3BDACF38DC5240C3BE6148D5ADE87DCA45ECE75C1420678E6");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("036484F12F2DFE83C7BB2BB8815AF4DB30CE9F8DF87912546B51634FCDB3C501");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }
        {
            final PrivateKey privateKey = PrivateKey.fromHexString("F56F7DBF8611DCA4D23253E1F832F8E5C9046C95E84BFF9048C56B69BF8B1988");
            wallet.addPrivateKey(privateKey);
            addresses.add(addressInflater.fromPrivateKey(privateKey));
            addresses.add(addressInflater.compressedFromPrivateKey(privateKey));
        }

        System.out.println("***** " + transactions.getSize());
        for (final Transaction transaction : transactions) {
            wallet.addTransaction(transaction);
            System.out.println(wallet.getBalance());
        }
    }
}
