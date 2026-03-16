package com.scaletoinsight.analytics.repository;

import com.scaletoinsight.analytics.model.FactSales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactSalesRepository extends JpaRepository<FactSales, Long> {

    List<FactSales> findByOrderId(String orderId);

    @Query("""
            SELECT fs FROM FactSales fs
            WHERE fs.dateKey IN (
                SELECT d.dateKey FROM DimDate d WHERE d.year = :year
            )
            """)
    List<FactSales> findByYear(@Param("year") int year);
}
