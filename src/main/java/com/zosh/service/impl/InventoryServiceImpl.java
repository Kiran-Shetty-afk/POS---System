package com.zosh.service.impl;


import com.zosh.exception.UserException;
import com.zosh.mapper.InventoryMapper;
import com.zosh.modal.Inventory;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.util.SecurityUtil;
import com.zosh.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryConsistencyService inventoryConsistencyService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public InventoryDTO createInventory(InventoryDTO dto) throws AccessDeniedException, UserException {
        validateCreateRequest(dto);
        return InventoryMapper.toDto(
                inventoryConsistencyService.addStock(dto.getBranchId(), dto.getProductId(), dto.getQuantity())
        );
    }

    @Override
    @Transactional
    public InventoryDTO updateInventory(Long id, InventoryDTO dto) throws AccessDeniedException, UserException {
        validateQuantity(dto.getQuantity());
        return InventoryMapper.toDto(inventoryConsistencyService.setQuantity(id, dto.getQuantity()));
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) throws AccessDeniedException, UserException {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));

        securityUtil.checkAuthority(inventory);

        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));

        return InventoryMapper.toDto(inventory);
    }

    @Override
    public List<InventoryDTO> getInventoryByBranch(Long branchId) {
        Map<Long, InventoryDTO> inventoryByProductId = new LinkedHashMap<>();

        for (Inventory inventory : inventoryRepository.findByBranchIdOrderByIdAsc(branchId)) {
            inventoryByProductId.compute(
                    inventory.getProduct().getId(),
                    (productId, existingInventory) -> mergeInventoryDto(existingInventory, inventory)
            );
        }

        return new ArrayList<>(inventoryByProductId.values());
    }

    @Override
    public InventoryDTO getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findFirstByProductIdOrderByIdAsc(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));

        return InventoryMapper.toDto(inventory);
    }

    @Override
    public InventoryDTO getInventoryByBranchAndProduct(Long branchId, Long productId) {
        List<Inventory> inventoryRows = inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId);
        if (inventoryRows.isEmpty()) {
            throw new EntityNotFoundException("Inventory not found for product in this branch");
        }

        Inventory primaryInventory = inventoryRows.get(0);
        return InventoryDTO.builder()
                .id(primaryInventory.getId())
                .branchId(primaryInventory.getBranch().getId())
                .productId(primaryInventory.getProduct().getId())
                .quantity(sumQuantities(inventoryRows))
                .build();
    }

    private void validateCreateRequest(InventoryDTO dto) {
        if (dto.getBranchId() == null) {
            throw new IllegalArgumentException("Branch id is required");
        }
        if (dto.getProductId() == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        validateQuantity(dto.getQuantity());
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    private InventoryDTO mergeInventoryDto(InventoryDTO existingInventory, Inventory inventory) {
        if (existingInventory == null) {
            return InventoryMapper.toDto(inventory);
        }

        existingInventory.setQuantity(
                safeQuantity(existingInventory.getQuantity()) + safeQuantity(inventory.getQuantity())
        );
        return existingInventory;
    }

    private int sumQuantities(List<Inventory> inventoryRows) {
        return inventoryRows.stream()
                .map(Inventory::getQuantity)
                .mapToInt(this::safeQuantity)
                .sum();
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }
}

