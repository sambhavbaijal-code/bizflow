package com.bizflow.repository;
import com.bizflow.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface BusinessRepository extends JpaRepository<Business,Long>{List<Business> findByOwner_IdOrderByNameAsc(Long ownerId);Optional<Business> findByIdAndOwner_Id(Long id,Long ownerId);}
