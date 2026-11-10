package com.bizflow.repository;
import com.bizflow.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PartyRepository extends JpaRepository<Party,Long>{
 List<Party> findByBusiness_IdOrderByNameAsc(Long businessId);
 List<Party> findByBusiness_IdAndTypeOrderByNameAsc(Long businessId,String type);
 Optional<Party> findByIdAndBusiness_Id(Long id,Long businessId);
 List<Party> findByBusinessIsNull();
}
