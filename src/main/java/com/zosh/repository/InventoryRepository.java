package com.zosh.repository;

import com.zosh.modal.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findFirstByProductIdOrderByIdAsc(Long productId);
    List<Inventory> findByBranchIdOrderByIdAsc(Long branchId);

    List<Inventory> findAllByBranchIdAndProductIdOrderByIdAsc(Long branchId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.branch.id = :branchId AND i.product.id = :productId
        ORDER BY i.id ASC
    """)
    List<Inventory> findAllByBranchIdAndProductIdForUpdate(@Param("branchId") Long branchId,
                                                           @Param("productId") Long productId);

    @Query("""
        SELECT COUNT(i)
        FROM Inventory i
        JOIN i.product p
        WHERE i.branch.id = :branchId
        AND i.quantity <= 5
    """)
    int countLowStockItems(@Param("branchId") Long branchId);

}
