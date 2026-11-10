package com.bizflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}
    public record ProductRequest(@NotBlank String sku, @NotBlank String name, String barcode, String category, String unit,
                                 String variant, @NotNull @DecimalMin("0.00") BigDecimal costPrice,
                                 @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
                                 @NotNull @DecimalMin("0.00") BigDecimal quantity,
                                 @NotNull @DecimalMin("0.00") BigDecimal reorderLevel) {}
    public record ProductResponse(Long id, String sku, String name, String barcode, String category, String unit, String variant,
                                  BigDecimal costPrice, BigDecimal sellingPrice, BigDecimal quantity,
                                  BigDecimal reorderLevel, boolean lowStock) {}
    public record PartyRequest(@NotBlank String name, @NotBlank String type, String phone, String email, String address) {}
    public record PartyResponse(Long id, String name, String type, String phone, String email, String address) {}
    public record LineRequest(@NotNull Long productId, @NotNull @DecimalMin(value="0.001") BigDecimal quantity,
                              @NotNull @DecimalMin("0.00") BigDecimal price) {}
    public record PurchaseRequest(@NotNull Long supplierId, @NotEmpty List<@Valid LineRequest> items, BigDecimal gstRate, String gstType) {}
    public record SaleRequest(Long customerId, @NotEmpty List<@Valid LineRequest> items, BigDecimal gstRate, String gstType) {}
    public record TransactionLineResponse(Long productId, String productName, String sku, BigDecimal quantity,
                                          BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record TransactionResponse(Long id, String invoiceNo, String partyName, LocalDateTime createdAt,
                                       BigDecimal subtotal, BigDecimal gstRate, BigDecimal gstAmount, String gstType,
                                       BigDecimal total, List<TransactionLineResponse> items) {}
    public record AssistantRequest(@NotBlank @Size(max = 1200) String question) {}
}
