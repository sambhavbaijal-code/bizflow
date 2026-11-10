package com.bizflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
public class SaleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Sale sale;
    @ManyToOne(optional = false) private Product product;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitCostAtSale = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal lineTotal;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Sale getSale() { return sale; } public void setSale(Sale sale) { this.sale = sale; }
    public Product getProduct() { return product; } public void setProduct(Product product) { this.product = product; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getUnitCostAtSale() { return unitCostAtSale; } public void setUnitCostAtSale(BigDecimal unitCostAtSale) { this.unitCostAtSale = unitCostAtSale; }
    public BigDecimal getLineTotal() { return lineTotal; } public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
