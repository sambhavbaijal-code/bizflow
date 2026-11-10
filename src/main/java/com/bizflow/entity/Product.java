package com.bizflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", uniqueConstraints = {@UniqueConstraint(columnNames={"business_id","sku"}), @UniqueConstraint(columnNames={"business_id","barcode"})})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=true) @JoinColumn(name="business_id",nullable=true) private Business business;
    @Column(nullable = false, length = 60) private String sku;
    @Column(length = 80) private String barcode;
    @Column(nullable = false, length = 180) private String name;
    @Column(length = 100) private String category;
    @Column(length = 30) private String unit;
    @Column(length = 120) private String variant;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal costPrice = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal sellingPrice = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal reorderLevel = BigDecimal.ZERO;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; } public void setSku(String sku) { this.sku = sku; }
    public String getBarcode() { return barcode; } public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; } public void setUnit(String unit) { this.unit = unit; }
    public String getVariant() { return variant; } public void setVariant(String variant) { this.variant = variant; }
    public BigDecimal getCostPrice() { return costPrice; } public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; } public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getReorderLevel() { return reorderLevel; } public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Business getBusiness() { return business; } public void setBusiness(Business business) { this.business = business; }
}
