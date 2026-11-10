package com.bizflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "parties")
public class Party {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=true) @JoinColumn(name="business_id",nullable=true) private Business business;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, length = 20) private String type; // CUSTOMER or SUPPLIER
    @Column(length = 30) private String phone;
    @Column(length = 160) private String email;
    @Column(length = 300) private String address;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public Business getBusiness() { return business; } public void setBusiness(Business business) { this.business = business; }
}
