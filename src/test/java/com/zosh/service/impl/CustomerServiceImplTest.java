package com.zosh.service.impl;

import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.Customer;
import com.zosh.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    void createCustomerDefaultsLoyaltyPointsToZero() {
        Customer customer = new Customer();
        customer.setFullName("Test Customer");
        customer.setPhone("9999999999");

        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer savedCustomer = customerService.createCustomer(customer);

        assertEquals(0, savedCustomer.getLoyaltyPoints());
    }

    @Test
    void addLoyaltyPointsIncrementsExistingBalance() throws ResourceNotFoundException {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFullName("Test Customer");
        customer.setLoyaltyPoints(7);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Customer updatedCustomer = customerService.addLoyaltyPoints(1L, 5);

        assertEquals(12, updatedCustomer.getLoyaltyPoints());
    }

    @Test
    void addLoyaltyPointsRejectsNonPositiveValues() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.addLoyaltyPoints(1L, 0)
        );

        assertEquals("Points must be greater than zero", exception.getMessage());
    }
}
