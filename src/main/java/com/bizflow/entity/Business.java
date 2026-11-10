package com.bizflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="businesses")
public class Business {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=160) private String name;
    @ManyToOne(optional=false) @JoinColumn(name="owner_id",nullable=false) private UserAccount owner;
    @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public UserAccount getOwner(){return owner;} public void setOwner(UserAccount v){owner=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
