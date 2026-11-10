package com.bizflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases", uniqueConstraints = @UniqueConstraint(columnNames={"business_id","invoice_no"}))
public class Purchase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=true) @JoinColumn(name="business_id",nullable=true) private Business business;
    @Column(nullable = false, length = 40) private String invoiceNo;
    @ManyToOne(optional = false) private Party supplier;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Column(precision = 5, scale = 2) private BigDecimal gstRate = BigDecimal.ZERO;
    @Column(precision = 14, scale = 2) private BigDecimal gstAmount = BigDecimal.ZERO;
    @Column(length = 8) private String gstType = "INTRA";
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true) private List<PurchaseItem> items = new ArrayList<>();
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getInvoiceNo() { return invoiceNo; } public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Party getSupplier() { return supplier; } public void setSupplier(Party supplier) { this.supplier = supplier; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BigDecimal getTotal() { return total; } public void setTotal(BigDecimal total) { this.total = total; }
    public List<PurchaseItem> getItems() { return items; } public void setItems(List<PurchaseItem> items) { this.items = items; }
    public Business getBusiness() { return business; } public void setBusiness(Business business) { this.business = business; }
    public BigDecimal getGstRate() { return gstRate==null?BigDecimal.ZERO:gstRate; } public void setGstRate(BigDecimal v) { gstRate=v; }
    public BigDecimal getGstAmount() { return gstAmount==null?BigDecimal.ZERO:gstAmount; } public void setGstAmount(BigDecimal v) { gstAmount=v; }
    public String getGstType() { return gstType==null?"INTRA":gstType; } public void setGstType(String v) { gstType=v; }
}
