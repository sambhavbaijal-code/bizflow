package com.bizflow.repository;
import com.bizflow.entity.Sale;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
public interface SaleRepository extends JpaRepository<Sale,Long>{
 List<Sale> findTop10ByBusiness_IdOrderByCreatedAtDesc(Long businessId);
 @Query("select coalesce(sum(s.total), 0) from Sale s where s.business.id=:businessId and s.createdAt >= :start and s.createdAt < :end") BigDecimal totalBetween(@Param("businessId") Long businessId,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
 @Query("select coalesce(sum(i.lineTotal - (i.quantity * i.unitCostAtSale)), 0) from SaleItem i where i.sale.business.id=:businessId and i.sale.createdAt >= :start and i.sale.createdAt < :end") BigDecimal grossProfitBetween(@Param("businessId") Long businessId,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
 List<Sale> findByBusinessIsNull();
}
