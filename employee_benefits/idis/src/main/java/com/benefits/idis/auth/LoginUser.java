package com.benefits.idis.auth;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.Role;

import java.io.Serializable;

public record LoginUser(String empNo, String name, Role role, String departmentName) implements Serializable {

    public static LoginUser from(Employee employee) {
        return new LoginUser(
                employee.getEmpNo(),
                employee.getName(),
                employee.getRole(),
                employee.getDepartment() == null ? null : employee.getDepartment().getName()
        );
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
