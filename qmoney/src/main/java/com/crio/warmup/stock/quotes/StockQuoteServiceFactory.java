
package com.crio.warmup.stock.quotes;

import org.springframework.web.client.RestTemplate;

public enum StockQuoteServiceFactory {

  INSTANCE;

  public StockQuotesService getService(String provider,  RestTemplate restTemplate) {

    System.out.println("\n\nValue of provider: " +provider +"\n\n");
    if (provider != null) {
      if (provider.equalsIgnoreCase("Tiingo")) {
        return new TiingoService(restTemplate);
      }
    } else {
      System.out.println("Provider is null! Return AlphavantageService instance.");
    }
     
    return new AlphavantageService(restTemplate);
  }

}
