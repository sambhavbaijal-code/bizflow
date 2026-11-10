package com.bizflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Purchase purchase;
    @ManyToOne(optional = false) private Product product;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitCost;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal lineTotal;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Purchase getPurchase() { return purchase; } public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public Product getProduct() { return product; } public void setProduct(Product product) { this.product = product; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; } public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; } public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
