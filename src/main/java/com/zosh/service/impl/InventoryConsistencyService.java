package com.zosh.service.impl;

import com.zosh.modal.Branch;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryConsistencyService {

    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Inventory addStock(Long branchId, Long productId, int quantityToAdd) {
        if (quantityToAdd < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        List<Inventory> inventoryRows = inventoryRepository.findAllByBranchIdAndProductIdForUpdate(branchId, productId);
        if (!inventoryRows.isEmpty()) {
            return mergeInventoryRows(inventoryRows, sumQuantities(inventoryRows) + quantityToAdd);
        }

        Branch branch = branchRepository.findByIdForUpdate(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        inventoryRows = inventoryRepository.findAllByBranchIdAndProductIdForUpdate(branchId, productId);
        if (!inventoryRows.isEmpty()) {
            return mergeInventoryRows(inventoryRows, sumQuantities(inventoryRows) + quantityToAdd);
        }

        Inventory inventory = Inventory.builder()
                .branch(branch)
                .product(product)
                .quantity(quantityToAdd)
                .build();

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory setQuantity(Long inventoryId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));

        List<Inventory> inventoryRows = inventoryRepository.findAllByBranchIdAndProductIdForUpdate(
                inventory.getBranch().getId(),
                inventory.getProduct().getId()
        );

        Inventory primary = inventoryRows.stream()
                .filter(row -> row.getId().equals(inventoryId))
                .findFirst()
                .orElse(inventory);

        return mergeInventoryRows(primary, inventoryRows, quantity);
    }

    @Transactional
    public Inventory decrementStock(Long branchId, Long productId, int quantityToDeduct) {
        if (quantityToDeduct <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        List<Inventory> inventoryRows = inventoryRepository.findAllByBranchIdAndProductIdForUpdate(branchId, productId);
        if (inventoryRows.isEmpty()) {
            throw new EntityNotFoundException("Inventory not found for product in this branch");
        }

        int totalAvailable = sumQuantities(inventoryRows);
        if (totalAvailable < quantityToDeduct) {
            throw new IllegalStateException("Insufficient stock for this product in the selected branch");
        }

        return mergeInventoryRows(inventoryRows, totalAvailable - quantityToDeduct);
    }

    @Transactional
    public int repairDuplicateInventories() {
        List<Inventory> allInventories = inventoryRepository.findAll();

        Map<String, List<Inventory>> groupedByBranchAndProduct = allInventories.stream()
                .collect(Collectors.groupingBy(this::buildGroupKey));

        int repairedGroups = 0;
        for (List<Inventory> inventoryRows : groupedByBranchAndProduct.values()) {
            if (inventoryRows.size() <= 1) {
                continue;
            }

            inventoryRows.sort(Comparator.comparing(Inventory::getId));
            mergeInventoryRows(inventoryRows, sumQuantities(inventoryRows));
            repairedGroups++;
        }

        return repairedGroups;
    }

    private Inventory mergeInventoryRows(List<Inventory> inventoryRows, int finalQuantity) {
        return mergeInventoryRows(inventoryRows.get(0), inventoryRows, finalQuantity);
    }

    private Inventory mergeInventoryRows(Inventory primary, List<Inventory> inventoryRows, int finalQuantity) {
        primary.setQuantity(finalQuantity);
        Inventory savedInventory = inventoryRepository.save(primary);

        List<Inventory> duplicatesToDelete = inventoryRows.stream()
                .filter(row -> !row.getId().equals(savedInventory.getId()))
                .toList();

        if (!duplicatesToDelete.isEmpty()) {
            inventoryRepository.deleteAllInBatch(duplicatesToDelete);
        }

        return savedInventory;
    }

    private int sumQuantities(List<Inventory> inventoryRows) {
        return inventoryRows.stream()
                .map(Inventory::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private String buildGroupKey(Inventory inventory) {
        return inventory.getBranch().getId() + ":" + inventory.getProduct().getId();
    }
}
