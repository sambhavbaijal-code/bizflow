package com.bizflow.controller;

import com.bizflow.dto.ApiDtos.*;
import com.bizflow.service.BizFlowService;
import com.bizflow.service.BharatCodeAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final BizFlowService service;
    private final BharatCodeAssistantService assistant;
    public ApiController(BizFlowService service, BharatCodeAssistantService assistant) { this.service = service; this.assistant = assistant; }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok", "app", "BizFlow"); }
    @GetMapping("/assistant/status") public Map<String, Object> assistantStatus() { return assistant.status(); }
    @PostMapping("/assistant") public Map<String, String> askAssistant(@Valid @RequestBody AssistantRequest request) {
        return Map.of("answer", assistant.ask(request.question()));
    }
    @GetMapping("/dashboard") public Map<String, Object> dashboard() { return service.dashboard(); }
    @GetMapping("/products") public List<ProductResponse> products() { return service.getProducts(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) { return service.createProduct(request); }
    @PutMapping("/products/{id}") public ProductResponse updateProduct(@PathVariable("id") Long id, @Valid @RequestBody ProductRequest request) { return service.updateProduct(id, request); }
    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteProduct(@PathVariable("id") Long id) { service.deactivateProduct(id); }
    @GetMapping("/parties") public List<PartyResponse> parties(@RequestParam(name = "type", required = false) String type) { return service.getParties(type); }
    @PostMapping("/parties") @ResponseStatus(HttpStatus.CREATED)
    public PartyResponse createParty(@Valid @RequestBody PartyRequest request) { return service.createParty(request); }
    @PostMapping("/purchases") @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createPurchase(@Valid @RequestBody PurchaseRequest request) { return service.createPurchase(request); }
    @GetMapping("/purchases/recent") public List<TransactionResponse> purchases() { return service.getRecentPurchases(); }
    @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createSale(@Valid @RequestBody SaleRequest request) { return service.createSale(request); }
    @GetMapping("/sales/recent") public List<TransactionResponse> sales() { return service.getRecentSales(); }
}
