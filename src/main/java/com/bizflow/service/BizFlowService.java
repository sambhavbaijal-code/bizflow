package com.bizflow.service;

import com.bizflow.dto.ApiDtos.*;
import com.bizflow.entity.*;
import com.bizflow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class BizFlowService {
    private final ProductRepository products;
    private final PartyRepository parties;
    private final PurchaseRepository purchases;
    private final SaleRepository sales;
    private final com.bizflow.auth.TenantContext tenant;

    public BizFlowService(ProductRepository products, PartyRepository parties,
                          PurchaseRepository purchases, SaleRepository sales, com.bizflow.auth.TenantContext tenant) {
        this.products = products; this.parties = parties; this.purchases = purchases; this.sales = sales; this.tenant = tenant;
    }

    public List<ProductResponse> getProducts() {
        return products.findByBusiness_IdAndActiveTrueOrderByNameAsc(tenant.id()).stream().map(this::productResponse).toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (products.findByBusiness_IdAndSkuIgnoreCase(tenant.id(), req.sku()).isPresent()) throw new IllegalArgumentException("SKU already exists.");
        ensureBarcodeAvailable(req.barcode(), null);
        Product p = new Product(); p.setBusiness(tenant.business()); applyProduct(p, req); p.setActive(true);
        return productResponse(products.save(p));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product p = products.findByIdAndBusiness_Id(id, tenant.id()).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        products.findByBusiness_IdAndSkuIgnoreCase(tenant.id(), req.sku()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalArgumentException("SKU already belongs to another product."); });
        ensureBarcodeAvailable(req.barcode(), id);
        applyProduct(p, req);
        return productResponse(products.save(p));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product p = products.findByIdAndBusiness_Id(id, tenant.id()).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        p.setActive(false); products.save(p);
    }

    private void applyProduct(Product p, ProductRequest r) {
        String barcode = r.barcode() == null ? null : r.barcode().trim();
        p.setSku(r.sku().trim()); p.setBarcode(barcode == null || barcode.isEmpty() ? null : barcode); p.setName(r.name().trim()); p.setCategory(clean(r.category()));
        p.setUnit(clean(r.unit())); p.setVariant(clean(r.variant())); p.setCostPrice(r.costPrice());
        p.setSellingPrice(r.sellingPrice()); p.setQuantity(r.quantity()); p.setReorderLevel(r.reorderLevel());
    }

    private void ensureBarcodeAvailable(String barcode, Long currentId) {
        String value = clean(barcode);
        if (value.isEmpty()) return;
        if (!value.isEmpty()) products.findByBusiness_IdAndBarcodeIgnoreCase(tenant.id(), value)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Barcode already belongs to another product."); });
    }

    public List<PartyResponse> getParties(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!t.isEmpty() && !t.equals("CUSTOMER") && !t.equals("SUPPLIER")) throw new IllegalArgumentException("Party type must be CUSTOMER or SUPPLIER.");
        List<Party> list = t.isEmpty() ? parties.findByBusiness_IdOrderByNameAsc(tenant.id()) : parties.findByBusiness_IdAndTypeOrderByNameAsc(tenant.id(), t);
        return list.stream().map(this::partyResponse).toList();
    }

    @Transactional
    public PartyResponse createParty(PartyRequest req) {
        String type = req.type().trim().toUpperCase(Locale.ROOT);
        if (!type.equals("CUSTOMER") && !type.equals("SUPPLIER")) throw new IllegalArgumentException("Party type must be CUSTOMER or SUPPLIER.");
        Party p = new Party(); p.setBusiness(tenant.business()); p.setName(req.name().trim()); p.setType(type); p.setPhone(clean(req.phone()));
        p.setEmail(clean(req.email())); p.setAddress(clean(req.address()));
        return partyResponse(parties.save(p));
    }

    @Transactional
    public TransactionResponse createPurchase(PurchaseRequest req) {
        Party supplier = parties.findByIdAndBusiness_Id(req.supplierId(), tenant.id()).orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
        if (!"SUPPLIER".equals(supplier.getType())) throw new IllegalArgumentException("Selected party is not a supplier.");
        Purchase p = new Purchase(); p.setBusiness(tenant.business()); p.setSupplier(supplier);
        BigDecimal gstRate = validGstRate(req.gstRate()); String gstType = validGstType(req.gstType());
        p.setGstRate(gstRate); p.setGstType(gstType);
        p.setInvoiceNo("PUR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseItem> lines = new ArrayList<>();
        for (LineRequest line : req.items()) {
            Product product = products.findByIdAndBusiness_Id(line.productId(), tenant.id()).orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));
            if (!product.isActive()) throw new IllegalArgumentException("Inactive product: " + product.getName());
            BigDecimal oldQty = product.getQuantity();
            BigDecimal newQty = oldQty.add(line.quantity());
            BigDecimal oldValue = oldQty.multiply(product.getCostPrice());
            BigDecimal incomingValue = line.quantity().multiply(line.price());
            BigDecimal weightedCost = newQty.compareTo(BigDecimal.ZERO) == 0 ? line.price()
                    : oldValue.add(incomingValue).divide(newQty, 2, RoundingMode.HALF_UP);
            product.setQuantity(newQty); product.setCostPrice(weightedCost); products.save(product);
            PurchaseItem item = new PurchaseItem(); item.setPurchase(p); item.setProduct(product);
            item.setQuantity(line.quantity()); item.setUnitCost(line.price());
            item.setLineTotal(line.quantity().multiply(line.price()).setScale(2, RoundingMode.HALF_UP));
            total = total.add(item.getLineTotal()); lines.add(item);
        }
        BigDecimal gst = tax(total, gstRate); p.setGstAmount(gst); p.setTotal(total.add(gst)); p.setItems(lines);
        Purchase saved = purchases.save(p);
        return purchaseResponse(saved);
    }

    @Transactional
    public TransactionResponse createSale(SaleRequest req) {
        Party customer = null;
        if (req.customerId() != null) {
            customer = parties.findByIdAndBusiness_Id(req.customerId(), tenant.id()).orElseThrow(() -> new IllegalArgumentException("Customer not found."));
            if (!"CUSTOMER".equals(customer.getType())) throw new IllegalArgumentException("Selected party is not a customer.");
        }
        // Validate all lines before changing stock, so a failed sale cannot partially update inventory.
        Map<Long, Product> productMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> requested = new HashMap<>();
        for (LineRequest line : req.items()) {
            Product product = products.findByIdAndBusiness_Id(line.productId(), tenant.id()).orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.productId()));
            if (!product.isActive()) throw new IllegalArgumentException("Inactive product: " + product.getName());
            productMap.put(product.getId(), product);
            requested.merge(product.getId(), line.quantity(), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> e : requested.entrySet()) {
            Product product = productMap.get(e.getKey());
            if (product.getQuantity().compareTo(e.getValue()) < 0)
                throw new IllegalArgumentException("Insufficient stock for " + product.getName() + ". Available: " + product.getQuantity());
        }
        Sale sale = new Sale(); sale.setBusiness(tenant.business()); sale.setCustomer(customer);
        BigDecimal gstRate = validGstRate(req.gstRate()); String gstType = validGstType(req.gstType());
        sale.setGstRate(gstRate); sale.setGstType(gstType);
        sale.setInvoiceNo("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        BigDecimal total = BigDecimal.ZERO; List<SaleItem> lines = new ArrayList<>();
        for (LineRequest line : req.items()) {
            Product product = productMap.get(line.productId());
            product.setQuantity(product.getQuantity().subtract(line.quantity())); products.save(product);
            SaleItem item = new SaleItem(); item.setSale(sale); item.setProduct(product);
            item.setQuantity(line.quantity()); item.setUnitPrice(line.price()); item.setUnitCostAtSale(product.getCostPrice());
            item.setLineTotal(line.quantity().multiply(line.price()).setScale(2, RoundingMode.HALF_UP));
            total = total.add(item.getLineTotal()); lines.add(item);
        }
        BigDecimal gst = tax(total, gstRate); sale.setGstAmount(gst); sale.setTotal(total.add(gst)); sale.setItems(lines);
        return saleResponse(sales.save(sale));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentSales() {
        return sales.findTop10ByBusiness_IdOrderByCreatedAtDesc(tenant.id()).stream().map(this::saleResponse).toList();
    }
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentPurchases() {
        return purchases.findTop10ByBusiness_IdOrderByCreatedAtDesc(tenant.id()).stream().map(this::purchaseResponse).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<Product> active = products.findByBusiness_IdAndActiveTrueOrderByNameAsc(tenant.id());
        long lowStock = active.stream().filter(p -> p.getQuantity().compareTo(p.getReorderLevel()) <= 0).count();
        BigDecimal inventoryValue = active.stream().map(p -> p.getQuantity().multiply(p.getCostPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now();
        BigDecimal todaySales = sales.totalBetween(tenant.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        BigDecimal todayPurchases = purchases.totalBetween(tenant.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productCount", active.size()); result.put("lowStockCount", lowStock);
        result.put("inventoryValue", inventoryValue); result.put("todaySales", todaySales);
        result.put("todayPurchases", todayPurchases); result.put("todayGrossProfit", sales.grossProfitBetween(tenant.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        result.put("recentSales", getRecentSales());
        result.put("lowStockProducts", active.stream().filter(p -> p.getQuantity().compareTo(p.getReorderLevel()) <= 0)
                .limit(6).map(this::productResponse).toList());
        return result;
    }

    private ProductResponse productResponse(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getBarcode(), p.getCategory(), p.getUnit(), p.getVariant(),
                p.getCostPrice(), p.getSellingPrice(), p.getQuantity(), p.getReorderLevel(),
                p.getQuantity().compareTo(p.getReorderLevel()) <= 0);
    }
    private PartyResponse partyResponse(Party p) {
        return new PartyResponse(p.getId(), p.getName(), p.getType(), p.getPhone(), p.getEmail(), p.getAddress());
    }
    private TransactionResponse purchaseResponse(Purchase p) {
        BigDecimal subtotal=p.getTotal().subtract(p.getGstAmount());
        return new TransactionResponse(p.getId(), p.getInvoiceNo(), p.getSupplier().getName(), p.getCreatedAt(), subtotal, p.getGstRate(), p.getGstAmount(), p.getGstType(), p.getTotal(),
                p.getItems().stream().map(i -> new TransactionLineResponse(i.getProduct().getId(), i.getProduct().getName(), i.getProduct().getSku(), i.getQuantity(), i.getUnitCost(), i.getLineTotal())).toList());
    }
    private TransactionResponse saleResponse(Sale s) {
        BigDecimal subtotal=s.getTotal().subtract(s.getGstAmount());
        return new TransactionResponse(s.getId(), s.getInvoiceNo(), s.getCustomer() == null ? "Walk-in customer" : s.getCustomer().getName(), s.getCreatedAt(), subtotal, s.getGstRate(), s.getGstAmount(), s.getGstType(), s.getTotal(),
                s.getItems().stream().map(i -> new TransactionLineResponse(i.getProduct().getId(), i.getProduct().getName(), i.getProduct().getSku(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList());
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private BigDecimal validGstRate(BigDecimal rate) { BigDecimal value=rate==null?BigDecimal.ZERO:rate; if(value.signum()<0||value.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("GST rate must be between 0 and 100%."); return value.setScale(2,RoundingMode.HALF_UP); }
    private String validGstType(String type) { String value=type==null?"INTRA":type.trim().toUpperCase(Locale.ROOT); if(!value.equals("INTRA")&&!value.equals("INTER"))throw new IllegalArgumentException("GST type must be INTRA or INTER."); return value; }
    private BigDecimal tax(BigDecimal amount, BigDecimal rate) { return amount.multiply(rate).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP); }
}
