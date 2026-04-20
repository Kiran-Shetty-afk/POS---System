package com.zosh.service.impl;

import com.zosh.domain.PaymentType;
import com.zosh.domain.UserRole;
import com.zosh.exception.UserException;
import com.zosh.modal.Branch;
import com.zosh.modal.Category;
import com.zosh.modal.Customer;
import com.zosh.modal.Inventory;
import com.zosh.modal.Order;
import com.zosh.modal.Product;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.payload.dto.OrderItemDTO;
import com.zosh.repository.BranchRepository;
import com.zosh.repository.CustomerRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplLoyaltyTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InventoryConsistencyService inventoryConsistencyService;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderAwardsLoyaltyPointsToAttachedCustomer() throws UserException {
        Branch branch = Branch.builder().id(3L).build();

        User cashier = new User();
        cashier.setId(11L);
        cashier.setRole(UserRole.ROLE_BRANCH_CASHIER);
        cashier.setBranch(branch);

        Customer persistedCustomer = new Customer();
        persistedCustomer.setId(5L);
        persistedCustomer.setFullName("Priya");
        persistedCustomer.setLoyaltyPoints(10);

        Customer requestedCustomer = new Customer();
        requestedCustomer.setId(5L);

        Product product = Product.builder()
                .id(7L)
                .name("Coffee")
                .sku("COF-1")
                .mrp(300.0)
                .sellingPrice(250.0)
                .category(Category.builder().id(9L).name("Beverages").build())
                .store(Store.builder().id(4L).brand("Zosh").build())
                .build();

        OrderDTO dto = OrderDTO.builder()
                .customer(requestedCustomer)
                .paymentType(PaymentType.CASH)
                .items(List.of(OrderItemDTO.builder()
                        .productId(7L)
                        .quantity(2)
                        .build()))
                .build();

        when(userService.getCurrentUser()).thenReturn(cashier);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(persistedCustomer));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(inventoryConsistencyService.decrementStock(3L, 7L, 2))
                .thenReturn(Inventory.builder().id(1L).quantity(20).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(99L);
            savedOrder.setCreatedAt(LocalDateTime.now());
            return savedOrder;
        });

        OrderDTO savedOrder = orderService.createOrder(dto);

        assertEquals(500.0, savedOrder.getTotalAmount());
        assertEquals(15, persistedCustomer.getLoyaltyPoints());
        assertEquals(15, savedOrder.getCustomer().getLoyaltyPoints());
        verify(customerRepository).findById(5L);
    }
}
