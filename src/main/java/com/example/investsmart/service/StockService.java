package com.example.investsmart.service;

import com.example.investsmart.entity.Stock;
import com.example.investsmart.exception.ResourceNotFoundException;
import com.example.investsmart.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {
     private final StockRepository stockRepository;

     public StockService(StockRepository stockRepository)
     {
        this.stockRepository = stockRepository;
     }

     public List<Stock> getStocks()
     {
         return stockRepository.findAll();
     }

     public  Stock createStock(Stock stock)
     {
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