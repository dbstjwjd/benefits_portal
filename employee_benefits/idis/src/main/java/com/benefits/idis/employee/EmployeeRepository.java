package com.benefits.idis.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findByNameAndActiveTrue(String name);
    boolean existsByPhone(String phone);
    long countByActiveTrue();
    long countByActiveTrueAndType(EmployeeType type);
    List<Employee> findByActiveTrueOrderByDepartmentNameAscNameAsc();
}
