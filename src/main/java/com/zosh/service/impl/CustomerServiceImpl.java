package com.zosh.service.impl;


import com.zosh.exception.ResourceNotFoundException;
import com.zosh.modal.Customer;
import com.zosh.repository.CustomerRepository;
import com.zosh.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer) {
        if (customer.getLoyaltyPoints() == null) {
            customer.setLoyaltyPoints(0);
        }
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customerData) throws ResourceNotFoundException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Customer not found with id " + id));

        customer.setFullName(customerData.getFullName());
        customer.setEmail(customerData.getEmail());
        customer.setPhone(customerData.getPhone());
        if (customerData.getLoyaltyPoints() != null) {
            customer.setLoyaltyPoints(customerData.getLoyaltyPoints());
        }

        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(Long id) throws ResourceNotFoundException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
        customerRepository.delete(customer);
    }

    @Override
    public Customer getCustomerById(Long id) throws ResourceNotFoundException {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public List<Customer> searchCustomer(String keyword) {
        return customerRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    @Override
    public Customer addLoyaltyPoints(Long id, Integer points) throws ResourceNotFoundException {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("Points must be greater than zero");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));

        customer.setLoyaltyPoints((customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints()) + points);
        return customerRepository.save(customer);
    }

}
