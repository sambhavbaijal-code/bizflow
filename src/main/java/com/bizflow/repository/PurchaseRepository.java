package com.bizflow.repository;
import com.bizflow.entity.Purchase;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
public interface PurchaseRepository extends JpaRepository<Purchase,Long>{
 List<Purchase> findTop10ByBusiness_IdOrderByCreatedAtDesc(Long businessId);
 @Query("select coalesce(sum(p.total), 0) from Purchase p where p.business.id=:businessId and p.createdAt >= :start and p.createdAt < :end") BigDecimal totalBetween(@Param("businessId") Long businessId,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
 List<Purchase> findByBusinessIsNull();
}
