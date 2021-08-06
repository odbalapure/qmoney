
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;

  public AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    System.out.println("Value: AlphavantageService.restTemplate: " +this.restTemplate);
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, 
    StockQuoteServiceException {
    System.out.println("Inside AlphavantageService::getStockQuote");

    List<Candle> candles = new ArrayList<>();
    Map<LocalDate, AlphavantageCandle> sortedMap = new TreeMap<>();

    try {
      String response = restTemplate.getForObject("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&" 
          +symbol +"=GPV.TRV&outputsize=full&apikey=Z3TOMZXUGY3OXJ7V" , String.class);
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.registerModule(new JavaTimeModule());
      AlphavantageDailyResponse alphavantageDailyResponse = objectMapper.readValue(response, AlphavantageDailyResponse.class);  
      if (alphavantageDailyResponse == null) {
        System.out.println("AlphavantageDailyResponse was NULL!");
        return Collections.emptyList();
      }    

      if (alphavantageDailyResponse != null) {
	    	System.out.println("\n\n\n\t\t\t\t********************** Unsorted Candle **********************");
	    	for (Map.Entry<LocalDate, AlphavantageCandle> mapElement : alphavantageDailyResponse.getCandles().entrySet()) {
	    		sortedMap.put(mapElement.getKey(), mapElement.getValue());
	    		System.out.println("( Open: " +mapElement.getValue().getOpen() +" | Close: " +mapElement.getValue().getClose() +" )");
	    	}
	    }
	    
	    for (Map.Entry<LocalDate, AlphavantageCandle> mapElement : sortedMap.entrySet()) {
	    	LocalDate dateBetween = mapElement.getKey();
	    	if (from != null && to != null && dateBetween != null) {
	    		if (dateBetween.isAfter(from) && dateBetween.isBefore(to) || dateBetween.isEqual(from)
	    				|| dateBetween.isEqual(to)) {
	    			mapElement.getValue().setDate(dateBetween);
	    			candles.add(mapElement.getValue());
	    		}
	    	} 
      }
    } catch (Exception e) {
      throw new StockQuoteServiceException("AlphaVantage Service is DOWN!", e);
    }
  
    System.out.println("\nSorted candles array...");
    for (Candle candle : candles) {
      System.out.println("( " +candle.getOpen() +" | " +candle.getClose() +" | " +candle.getHigh() +" | " +candle.getLow() +" )");
    }

    return candles;
  }

}
