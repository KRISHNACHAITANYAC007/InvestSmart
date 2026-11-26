package com.example.investsmart.trading;

import java.math.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Explicit educational settings, never advertised as broker eligibility or statutory tax rates. */
@Component
public class PaperPolicy {
    public static final String VERSION="paper-20261111-v1";
    public final BigDecimal leverage,annualInterest,feeRate,feeCap,protection,maintenance;
    public PaperPolicy(@Value("${investsmart.paper.leverage:2}") BigDecimal leverage,
            @Value("${investsmart.paper.annual-interest:0.12}") BigDecimal interest,
            @Value("${investsmart.paper.fee-rate:0.0003}") BigDecimal rate,
            @Value("${investsmart.paper.fee-cap:20}") BigDecimal cap){
        if(leverage.compareTo(BigDecimal.ONE)<0||leverage.compareTo(new BigDecimal("2"))>0||interest.signum()<0||interest.compareTo(BigDecimal.ONE)>0||rate.signum()<0||rate.compareTo(new BigDecimal("0.1"))>0||cap.signum()<0)throw new IllegalStateException("Invalid paper simulation policy");
        this.leverage=leverage;annualInterest=interest;feeRate=rate;feeCap=cap;protection=new BigDecimal("0.05");maintenance=new BigDecimal("0.25");
    }
    public static BigDecimal money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    public String version(){return VERSION+"-"+Integer.toHexString((leverage.toPlainString()+":"+annualInterest+":"+feeRate+":"+feeCap).hashCode());}
    public BigDecimal fee(BigDecimal value){return money(value.multiply(feeRate).min(feeCap));}
    public BigDecimal contribution(BigDecimal value){return value.divide(leverage,2,RoundingMode.CEILING);}
}
