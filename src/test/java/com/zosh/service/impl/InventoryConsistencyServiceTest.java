package com.zosh.service.impl;

import com.zosh.modal.Branch;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryConsistencyServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryConsistencyService inventoryConsistencyService;

    @Test
    void addStockMergesDuplicateRowsIntoOneInventoryRecord() {
        Inventory firstRow = inventory(4L, 1L, 4L, 0);
        Inventory duplicateRow = inventory(102L, 1L, 4L, 18);

        when(inventoryRepository.findAllByBranchIdAndProductIdForUpdate(1L, 4L))
                .thenReturn(List.of(firstRow, duplicateRow));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inventory savedInventory = inventoryConsistencyService.addStock(1L, 4L, 2);

        assertEquals(20, savedInventory.getQuantity());
        verify(branchRepository, never()).findByIdForUpdate(any());
        verify(productRepository, never()).findById(any());
        assertDeletedRows(102L);
    }

    @Test
    void decrementStockUsesMergedQuantityInsteadOfDeductingFromEveryDuplicateRow() {
        Inventory firstRow = inventory(4L, 1L, 4L, 0);
        Inventory duplicateRow = inventory(102L, 1L, 4L, 18);

        when(inventoryRepository.findAllByBranchIdAndProductIdForUpdate(1L, 4L))
                .thenReturn(List.of(firstRow, duplicateRow));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inventory savedInventory = inventoryConsistencyService.decrementStock(1L, 4L, 2);

        assertEquals(16, savedInventory.getQuantity());
        assertDeletedRows(102L);
    }

    @Test
    void repairDuplicateInventoriesMergesExistingDuplicateGroups() {
        Inventory firstRow = inventory(4L, 1L, 4L, 0);
        Inventory duplicateRow = inventory(102L, 1L, 4L, 18);
        Inventory normalRow = inventory(1L, 1L, 1L, 20);

        when(inventoryRepository.findAll()).thenReturn(List.of(firstRow, duplicateRow, normalRow));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int repairedGroups = inventoryConsistencyService.repairDuplicateInventories();

        assertEquals(1, repairedGroups);
        assertDeletedRows(102L);
    }

    private void assertDeletedRows(Long... expectedIds) {
        ArgumentCaptor<List<Inventory>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryRepository).deleteAllInBatch(captor.capture());
        List<Inventory> deletedRows = captor.getValue();
        assertEquals(expectedIds.length, deletedRows.size());
        for (int index = 0; index < expectedIds.length; index++) {
            assertEquals(expectedIds[index], deletedRows.get(index).getId());
        }
    }

    private Inventory inventory(Long inventoryId, Long branchId, Long productId, int quantity) {
        return Inventory.builder()
                .id(inventoryId)
                .branch(Branch.builder().id(branchId).build())
                .product(Product.builder().id(productId).name("Product " + productId).build())
                .quantity(quantity)
                .build();
    }
}
