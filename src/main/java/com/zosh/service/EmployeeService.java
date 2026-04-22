package com.zosh.service;

import com.zosh.domain.UserRole;
import com.zosh.modal.User;
import com.zosh.payload.dto.UserDTO;

import java.util.List;

public interface EmployeeService {
    UserDTO createStoreEmployee(UserDTO employee, Long storeId) throws Exception;
    User createBranchEmployee(User employee, Long branchId) throws Exception;
    User updateEmployee(Long employeeId, User employeeDetails) throws Exception;
    void deleteEmployee(Long employeeId) throws Exception;
    User findEmployeeById(Long employeeId) throws Exception;
    List<UserDTO> findStoreEmployees(Long storeId, UserRole role) throws Exception;
    List<UserDTO> findBranchEmployees(Long branchId, UserRole role) throws Exception;
}