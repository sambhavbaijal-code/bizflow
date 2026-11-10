package com.bizflow;

import com.bizflow.entity.Party;
import com.bizflow.entity.Product;
import com.bizflow.repository.PartyRepository;
import com.bizflow.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class BizFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(BizFlowApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDemoData(ProductRepository products, PartyRepository parties) {
        return args -> {
            if (products.count() == 0) {
                products.save(product("TSH-001", "Classic Cotton T-Shirt", "Clothing", "piece", "M / Black", "399", "699", "18", "5"));
                products.save(product("MOU-002", "Wireless Mouse", "Electronics", "piece", "Black", "420", "699", "12", "4"));
                products.save(product("RIC-003", "Basmati Rice 5 kg", "Grocery", "bag", "5 kg pack", "310", "399", "25", "8"));
            }
            if (parties.count() == 0) {
                Party supplier = new Party();
                supplier.setName("Demo Supplier"); supplier.setType("SUPPLIER"); supplier.setPhone("9876543210"); supplier.setEmail("supplier@example.com");
                parties.save(supplier);
                Party customer = new Party();
                customer.setName("Walk-in Customer"); customer.setType("CUSTOMER"); customer.setPhone(""); customer.setEmail("");
                parties.save(customer);
            }
        };
    }

    private Product product(String sku, String name, String category, String unit, String variant,
                            String cost, String price, String qty, String reorder) {
        Product p = new Product();
        p.setSku(sku); p.setName(name); p.setCategory(category); p.setUnit(unit); p.setVariant(variant);
        p.setCostPrice(new BigDecimal(cost)); p.setSellingPrice(new BigDecimal(price));
        p.setQuantity(new BigDecimal(qty)); p.setReorderLevel(new BigDecimal(reorder)); p.setActive(true);
        return p;
    }
}
