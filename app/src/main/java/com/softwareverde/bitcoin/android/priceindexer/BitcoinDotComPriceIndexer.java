package com.softwareverde.bitcoin.android.priceindexer;

import com.softwareverde.bitcoin.android.lib.BitcoinVerde;
import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.HttpRequest;
import com.softwareverde.http.HttpResponse;
import com.softwareverde.http.WebRequest;
import com.softwareverde.json.Json;
import com.softwareverde.logging.Logger;
import com.softwareverde.util.Container;

public class BitcoinDotComPriceIndexer implements BitcoinVerde.PriceIndexer {
    @Override
    public Double getDollarsPerBitcoin() {
        final WebRequest webRequest = new WebRequest();
        webRequest.setUrl("https://index-api.bitcoin.com/api/v0/cash/price/usd");
        webRequest.setMethod(HttpMethod.GET);

        final Container<HttpResponse> httpResponse = new Container<HttpResponse>();
        webRequest.execute(new HttpRequest.Callback() {
            @Override
            public void run(final HttpResponse response) {
                synchronized (httpResponse) {
                    httpResponse.value = response;
                    httpResponse.notifyAll();
                }
            }
        });

        synchronized (httpResponse) {
            try {
                httpResponse.wait(5000L);

                final Json result = httpResponse.value.getJsonResult();
                final Long price = result.getLong("price");
                return (price / 100D);
            }
            catch (final Exception exception) {
                Logger.debug("Unable to load price index.");
                return 0D;
            }
        }
    }
}
