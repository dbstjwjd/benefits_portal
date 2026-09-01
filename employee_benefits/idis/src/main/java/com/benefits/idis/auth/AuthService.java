package com.benefits.idis.auth;

import com.benefits.idis.employee.Employee;
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

    /**
     * 이름 + 전화번호 확인. 관리자라면 여기서 끝이 아니라 PIN 단계가 더 있다.
     * 세션 생성 여부는 호출하는 쪽(AuthController)이 정한다.
     */
    public Optional<Employee> authenticate(String name, String phone) {
        String digits = onlyDigits(phone);
        if (name == null || name.isBlank() || digits.isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findByNameAndActiveTrue(name.strip()).stream()
                .filter(employee -> digits.equals(onlyDigits(employee.getPhone())))
                .findFirst();
    }

    /** PIN 을 마친 뒤 세션에 담을 직원을 다시 읽는다. */
    public Optional<Employee> findActive(String empNo) {
        return employeeRepository.findById(empNo).filter(Employee::isActive);
    }

    private static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
