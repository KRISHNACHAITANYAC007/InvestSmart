package com.example.investsmart.service;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.exception.BadRequestException;
import com.example.investsmart.market.MarketDataService;
import com.example.investsmart.market.MarketInstrument;
import com.example.investsmart.market.MarketQuote;
import com.example.investsmart.market.EquityCatalogService;
import com.example.investsmart.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class StockService {
     private static final Logger log = LoggerFactory.getLogger(StockService.class);
     private final StockRepository stockRepository;
     private final MarketDataService marketDataService;
     private final EquityCatalogService catalog;

     public StockService(
             StockRepository stockRepository,
             MarketDataService marketDataService, EquityCatalogService catalog)
     {
        this.stockRepository = stockRepository;
        this.marketDataService = marketDataService;
        this.catalog = catalog;
     }

     public List<Stock> getStocks()
     {
         return stockRepository.findAll();
     }

     public  Stock createStock(Stock stock)
     {
         return stockRepository.save(stock);
     }

    public Stock importNseStock(String instrumentKey, String symbol) {
        MarketInstrument instrument = catalog.resolve(instrumentKey);
        if (!instrument.getSymbol().equalsIgnoreCase(symbol))
            throw new BadRequestException("Stock symbol does not match the selected listing.");

        Stock stock = stockRepository.findByInstrumentKey(instrumentKey)
                .or(() -> "NSE".equals(instrument.getExchange())
                        ? stockRepository.findBySymbolIgnoreCaseAndInstrumentKeyIsNull(symbol) : java.util.Optional.empty())
                .orElseGet(Stock::new);

        stock.setInstrumentKey(instrumentKey);
        stock.setSymbol(instrument.getSymbol());
        stock.setCompanyName(instrument.getCompanyName());
        stock.setExchange(instrument.getExchange());
        stock.setIsin(instrument.getIsin());
        try {
            MarketQuote quote = marketDataService.getQuote(instrument);
            if (quote != null && quote.getCurrentPrice() != null) {
                stock.setCurrentPrice(quote.getCurrentPrice());
                stock.setPreviousClose(quote.getPreviousClose());
                stock.setDayHigh(quote.getDayHigh());
                stock.setDayLow(quote.getDayLow());
                stock.setMarketDataUpdatedAt(quote.getLastUpdated());
            }
        } catch (RuntimeException quoteFailure) {
            log.info("Quote unavailable while adding {} instrument {}; retaining saved prices", instrument.getExchange(), symbol);
        }
        return stockRepository.save(stock);
    }

    public Stock getStockById(Long id) {

        return stockRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found"
                        )
                );
    }
     public Stock updateStock(Long id, Stock stock)
     {
         Stock existingStock =
                 stockRepository.findById(id)
                         .orElseThrow(() ->
                                 new ResourceNotFoundException(
                                         "Stock not found"
                                 )
                         );
         existingStock.setSymbol(stock.getSymbol());
         existingStock.setCompanyName(stock.getCompanyName());
         existingStock.setSector(stock.getSector());
         existingStock.setCurrentPrice(stock.getCurrentPrice());
         existingStock.setPreviousClose(stock.getPreviousClose());
         existingStock.setDayHigh(stock.getDayHigh());
         existingStock.setDayLow(stock.getDayLow());

         return stockRepository.save(existingStock);
     }

    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }
}
