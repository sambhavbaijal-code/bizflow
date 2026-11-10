package com.bizflow.repository;
import com.bizflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,Long>{
 List<Product> findByBusiness_IdAndActiveTrueOrderByNameAsc(Long businessId);
 Optional<Product> findByBusiness_IdAndSkuIgnoreCase(Long businessId,String sku);
 Optional<Product> findByBusiness_IdAndBarcodeIgnoreCase(Long businessId,String barcode);
 Optional<Product> findByIdAndBusiness_Id(Long id,Long businessId);
 List<Product> findByBusinessIsNull();
}
