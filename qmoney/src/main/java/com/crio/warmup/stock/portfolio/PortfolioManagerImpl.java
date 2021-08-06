
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.stream.Collectors;

import com.crio.warmup.stock.exception.StockQuoteServiceException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  public PortfolioManagerImpl(String provider, RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    stockQuotesService = StockQuoteServiceFactory.INSTANCE.getService(provider, this.restTemplate);
    System.out.println("Value of PortfolioManagerImpl.restTemplate: " +this.restTemplate);
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException,
      StockQuoteServiceException {
    System.out.println("Inside PorfolioManagerImpl::getStockQuote()");
    
    List<Candle> listCandles = stockQuotesService.getStockQuote(symbol, from, to);

    if (listCandles == null) {
      System.out.println("Candles are empty!");
      return new ArrayList<>();
    }  

    return listCandles;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws StockQuoteServiceException {
    long start = System.currentTimeMillis();
    System.out.println("Inside PorfolioManagerImpl::calculateAnnualizedReturn()");
    List<AnnualizedReturn> annualizedReturnList = new ArrayList<>();
    List<Candle> candles = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      try {
        candles = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
      } catch (JsonProcessingException e) {
        System.out.println("Got an NPE!");
        e.printStackTrace();
      }

      double sellPrice = candles.get(0).getOpen();
      double buyPrice = candles.get(candles.size() - 1).getClose();

      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade, sellPrice, buyPrice);
      annualizedReturnList.add(annualizedReturn);
    }

    List<AnnualizedReturn> sortedList = annualizedReturnList.stream()
        .sorted(Comparator.comparingDouble(AnnualizedReturn::getAnnualizedReturn)
        .reversed())
        .collect(Collectors.toList());

    long end = System.currentTimeMillis();

    System.out.println("***************************************");
    System.out.println("Time to execute the function WITHOUT Multi-Threading: " +(end - start));
    System.out.println("***************************************");
    return sortedList;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {

    List<Future<AnnualizedReturn>> futureReturnsList = new
    ArrayList<Future<AnnualizedReturn>>();
    
    final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
    
    long start = System.currentTimeMillis();

    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      Callable<AnnualizedReturn> callableTask = () -> {
        List<Candle> candles = new ArrayList<>();
        candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
        double sellPrice = candles.get(0).getOpen();
        double buyPrice = candles.get(candles.size() - 1).getClose();
        return calculateAnnualizedReturns(endDate, trade, sellPrice, buyPrice);
      };

      Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
      futureReturnsList.add(futureReturns);
    }

    long end = System.currentTimeMillis();
    System.out.println("***************************************");
    System.out.println("Time to execute the function WITH Multi-Threading: " +(end - start));
    System.out.println("***************************************");

    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (int i = 0; i < portfolioTrades.size(); i++) {
      Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
      try {
        AnnualizedReturn returns = futureReturns.get();
        annualizedReturns.add(returns);
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("Error when calling the API", e);
      }
    }

    List<AnnualizedReturn> sortedList = annualizedReturns.stream()
        .sorted(Comparator.comparingDouble(AnnualizedReturn::getAnnualizedReturn)
        .reversed())
        .collect(Collectors.toList());

    
    return sortedList;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {

    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double numYears = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.24;
    double annualizedReturns = Math.pow((1 + totalReturns), (1 / numYears)) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }

}

