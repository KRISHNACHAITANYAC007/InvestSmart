package com.example.investsmart.investment;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="saved_interests",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","kind","reference_id"}))
public class SavedInterest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false,length=10) private String kind;
    @Column(name="reference_id",nullable=false,length=180) private String referenceId;
    private String name;
    private Instant savedAt;
    public Long getId(){return id;} public Long getUserId(){return userId;}
    public String getKind(){return kind;} public String getReferenceId(){return referenceId;}
    public String getName(){return name;} public Instant getSavedAt(){return savedAt;}
    public void setUserId(Long v){userId=v;} public void setKind(String v){kind=v;}
    public void setReferenceId(String v){referenceId=v;} public void setName(String v){name=v;} public void setSavedAt(Instant v){savedAt=v;}
}
