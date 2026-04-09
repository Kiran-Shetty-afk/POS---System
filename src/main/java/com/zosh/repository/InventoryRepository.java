package com.zosh.repository;

import com.zosh.modal.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Inventory findByProductId(Long productId);
    List<Inventory> findByBranchId(Long branchId);

    Optional<Inventory> findByBranchIdAndProductId(Long branchId, Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Inventory i
        SET i.quantity = i.quantity - :qty
        WHERE i.branch.id = :branchId AND i.product.id = :productId AND i.quantity >= :qty
    """)
    int decrementIfEnough(@Param("branchId") Long branchId,
                          @Param("productId") Long productId,
                          @Param("qty") int qty);

    @Query("""
        SELECT COUNT(i)
        FROM Inventory i
        JOIN i.product p
        WHERE i.branch.id = :branchId
        AND i.quantity <= 5
    """)
    int countLowStockItems(@Param("branchId") Long branchId);

}
