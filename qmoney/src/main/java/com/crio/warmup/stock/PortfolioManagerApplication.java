
package com.crio.warmup.stock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;

import java.util.UUID;

import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.Include;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    ObjectMapper mapper = getObjectMapper();
    List<String> listSymbol = new ArrayList<>();
    File jsonFile = resolveFileFromResources(args[0]);
    PortfolioTrade[] portfolioTrades = mapper.readValue(jsonFile, PortfolioTrade[].class);

    for (PortfolioTrade symbol : portfolioTrades) {
      listSymbol.add(symbol.getSymbol());
    }

    return listSymbol;
  }

  private static void printJsonObject(Object object) throws IOException, URISyntaxException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {
    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/odbalapure-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@35e2d654";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "52";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    ObjectMapper mapper = getObjectMapper();

    List<String> sortedSymbolList = new ArrayList<>();
    List<TotalReturnsDto> totalReturnsDtosList = new ArrayList<>();

    File jsonFile = resolveFileFromResources(args[0]);
    PortfolioTrade[] portfolioTrades = mapper.readValue(jsonFile, PortfolioTrade[].class);

    LocalDate purchaseDate = null;
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      purchaseDate = portfolioTrade.getPurchaseDate();
    }

    // get stock list from a start date to an end date
    LocalDate startDate = purchaseDate;
    LocalDate endDate = LocalDate.parse(args[1]);

    // throw a run time exception if start date is greater than end date
    if (startDate != null) {
      if (startDate.isAfter(endDate)) {
        throw new RuntimeException("Start Date MUST be smaller than the End Date!");
      }
    }

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      // call the tiingo api for each symbol
      Candle[] candles = new RestTemplate().getForObject(
          generateUrl(portfolioTrade.getSymbol(), startDate.toString(), endDate.toString()), TiingoCandle[].class);
      totalReturnsDtosList.add(new TotalReturnsDto(portfolioTrade.getSymbol(), candles[candles.length - 1].getClose()));
    }

    // sort symbols by their closing prices
    Collections.sort(totalReturnsDtosList, new Include());

    for (TotalReturnsDto tReturnsDto : totalReturnsDtosList) {
      sortedSymbolList.add(tReturnsDto.getSymbol());
    }

    return sortedSymbolList;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws IOException, URISyntaxException {
    ObjectMapper mapper = getObjectMapper();
    List<AnnualizedReturn> annualizedReturnList = new ArrayList<>();
    File jsonFile = resolveFileFromResources(args[0]);
    PortfolioTrade[] portfolioTrades = mapper.readValue(jsonFile, PortfolioTrade[].class);
    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      // call the tiingo api for each symbol
      Candle[] candles = new RestTemplate().getForObject(
          generateUrl(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate().toString(), args[1]),
          TiingoCandle[].class);

      double openPrice = candles[0].getOpen();
      double closePrice = candles[candles.length - 1].getClose();

      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(LocalDate.parse(args[1]), portfolioTrade,
          openPrice, closePrice);
      annualizedReturnList.add(annualizedReturn);
    }
    // sort objects in reverse order based on annual returns
    Collections.sort(annualizedReturnList, Comparator.comparing((AnnualizedReturn::getAnnualizedReturn)).reversed());

    return annualizedReturnList;
  }

  // Annualized Returns formula is:
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {

    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double numYears = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.24;
    double annualizedReturns = Math.pow((1 + totalReturns), (1 / numYears)) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  // Helper method to generate url based on symbol, start and end date
  private static String generateUrl(String symbol, String start, String end) {
    return "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + start + "&endDate=" + end
        + "&token=0e3bf1653ea9a3f33d06648dab584b14295de9de";
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {

    ObjectMapper objectMapper = getObjectMapper();
    File jsonFile = resolveFileFromResources(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);

    PortfolioTrade[] portfolioTrades = objectMapper.readValue(jsonFile, PortfolioTrade[].class);
    
    RestTemplate restTemplate = new RestTemplate();
    PortfolioManager portfolioManagerImpl = PortfolioManagerFactory.getPortfolioManager("Tiingo", restTemplate);

    System.out.println("Value of PortfolioManagerApplication.restTemplate: " +restTemplate);

    // return portfolioManagerImpl.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
    return portfolioManagerImpl.calculateAnnualizedReturnParallel(Arrays.asList(portfolioTrades), endDate, 3);
  }

  public static void main(String[] args) throws Exception {

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));
    printJsonObject(mainReadQuotes(args));
    printJsonObject(mainCalculateSingleReturn(args));
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}