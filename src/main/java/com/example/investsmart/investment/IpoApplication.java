package com.example.investsmart.investment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity
@Table(name="ipo_applications",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","ipo_id"}))
public class IpoApplication {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="user_id",nullable=false)
    private Long userId;
    @Column(name="ipo_id",nullable=false,length=180)
    private String ipoId;
    
    private String name;
    
    private String symbol;
    
    private String isin;
    
    private String exchange;
    @Column(nullable=false)
    private String status;
    
    private Integer lots;
    
    private Integer lotSize;
    
    private Integer minimumQuantity;
    
    private Integer appliedQuantity;
    
    private Integer allottedQuantity;
    @Column(precision=19,scale=4)
    private BigDecimal maximumPrice;
    @Column(precision=19,scale=2)
    private BigDecimal reservedAmount;
    @Column(precision=19,scale=2)
    private BigDecimal allocatedCost;
    @Column(precision=19,scale=4)
    private BigDecimal issuePrice;
    @Column(precision=20,scale=18)
    private BigDecimal draw;
    @Column(precision=20,scale=18)
    private BigDecimal probability;
    @Column(precision=30,scale=8)
    private BigDecimal subscription;
    
    private String modelVersion;
    @Column(length=1200)
    private String reason;
    
    private LocalDate allotmentDate;
    
    private LocalDate listingDate;
    
    private Instant appliedAt;
    
    private Instant resolvedAt;
    
    private Instant sourceUpdatedAt;
    @Column(unique=true)
    private Long listingOrderId;
    
    private Boolean assumedIssuePrice;
    public IpoApplication() {}
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    public Long getUserId(){return userId;}
    public void setUserId(Long value){userId=value;}
    public String getIpoId(){return ipoId;}
    public void setIpoId(String value){ipoId=value;}
    public String getName(){return name;}
    public void setName(String value){name=value;}
    public String getSymbol(){return symbol;}
    public void setSymbol(String value){symbol=value;}
    public String getIsin(){return isin;}
    public void setIsin(String value){isin=value;}
    public String getExchange(){return exchange;}
    public void setExchange(String value){exchange=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){status=value;}
    public Integer getLots(){return lots;}
    public void setLots(Integer value){lots=value;}
    public Integer getLotSize(){return lotSize;}
    public void setLotSize(Integer value){lotSize=value;}
    public Integer getMinimumQuantity(){return minimumQuantity;}
    public void setMinimumQuantity(Integer value){minimumQuantity=value;}
    public Integer getAppliedQuantity(){return appliedQuantity;}
    public void setAppliedQuantity(Integer value){appliedQuantity=value;}
    public Integer getAllottedQuantity(){return allottedQuantity;}
    public void setAllottedQuantity(Integer value){allottedQuantity=value;}
    public BigDecimal getMaximumPrice(){return maximumPrice;}
    public void setMaximumPrice(BigDecimal value){maximumPrice=value;}
    public BigDecimal getReservedAmount(){return reservedAmount;}
    public void setReservedAmount(BigDecimal value){reservedAmount=value;}
    public BigDecimal getAllocatedCost(){return allocatedCost;}
    public void setAllocatedCost(BigDecimal value){allocatedCost=value;}
    public BigDecimal getIssuePrice(){return issuePrice;}
    public void setIssuePrice(BigDecimal value){issuePrice=value;}
    public BigDecimal getDraw(){return draw;}
    public void setDraw(BigDecimal value){draw=value;}
    public BigDecimal getProbability(){return probability;}
    public void setProbability(BigDecimal value){probability=value;}
    public BigDecimal getSubscription(){return subscription;}
    public void setSubscription(BigDecimal value){subscription=value;}
    public String getModelVersion(){return modelVersion;}
    public void setModelVersion(String value){modelVersion=value;}
    public String getReason(){return reason;}
    public void setReason(String value){reason=value;}
    public LocalDate getAllotmentDate(){return allotmentDate;}
    public void setAllotmentDate(LocalDate value){allotmentDate=value;}
    public LocalDate getListingDate(){return listingDate;}
    public void setListingDate(LocalDate value){listingDate=value;}
    public Instant getAppliedAt(){return appliedAt;}
    public void setAppliedAt(Instant value){appliedAt=value;}
    public Instant getResolvedAt(){return resolvedAt;}
    public void setResolvedAt(Instant value){resolvedAt=value;}
    public Instant getSourceUpdatedAt(){return sourceUpdatedAt;}
    public void setSourceUpdatedAt(Instant value){sourceUpdatedAt=value;}
    public Long getListingOrderId(){return listingOrderId;}
    public void setListingOrderId(Long value){listingOrderId=value;}
    public Boolean getAssumedIssuePrice(){return assumedIssuePrice;}
    public void setAssumedIssuePrice(Boolean value){assumedIssuePrice=value;}
}
