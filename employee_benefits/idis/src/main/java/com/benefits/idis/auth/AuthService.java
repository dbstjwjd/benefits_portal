package com.benefits.idis.auth;

import com.benefits.idis.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmployeeRepository employeeRepository;
    public Optional<LoginUser> authenticate(String name, String phone) {
        String digits = onlyDigits(phone);
        if (name == null || name.isBlank() || digits.isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findByNameAndActiveTrue(name.strip()).stream()
                .filter(employee -> digits.equals(onlyDigits(employee.getPhone())))
                .findFirst()
                .map(LoginUser::from);
    }

    private static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
