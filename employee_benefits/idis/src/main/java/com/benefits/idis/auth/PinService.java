package com.benefits.idis.auth;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 관리자 PIN 검증·발급.
 * 원문 PIN 은 이 클래스 밖으로 나가지 않고, 로그에도 남기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PinService {

    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPin) {
        if (!PinPolicy.isValidFormat(rawPin)) {
            throw new IllegalArgumentException(PinPolicy.formatMessage());
        }
        return encoder.encode(rawPin);
    }

    /** 로그인 화면의 PIN 확인. 잠금·실패 누적까지 여기서 처리한다. */
    @Transactional
    public Result verify(String empNo, String rawPin) {
        Employee employee = employeeRepository.findById(empNo).orElse(null);
        if (employee == null || !employee.isActive() || !employee.isAdmin()) {
            return Result.rejected("다시 로그인해 주세요");
        }
        if (!employee.hasPin()) {
            return Result.rejected("관리자에게 PIN 발급을 요청해 주세요");
        }
        if (employee.isPinLocked()) {
            return Result.rejected(lockMessage(employee.getPinLockedUntil()));
        }
        if (!PinPolicy.isValidFormat(rawPin) || !encoder.matches(rawPin, employee.getPinHash())) {
            employee.recordPinFailure(PinPolicy.MAX_FAIL, PinPolicy.LOCK_FOR);
            if (employee.isPinLocked()) {
                return Result.rejected(lockMessage(employee.getPinLockedUntil()));
            }
            return Result.rejected("PIN 이 일치하지 않습니다");
        }
        employee.recordPinSuccess();
        return Result.accepted(employee.isPinChangeRequired());
    }

    /** 본인 PIN 변경. 현재 PIN 을 확인한 뒤 새 값으로 바꾼다. */
    @Transactional
    public void change(String empNo, String currentPin, String newPin, String confirmPin) {
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다"));
        if (employee.isPinLocked()) {
            throw new IllegalArgumentException(lockMessage(employee.getPinLockedUntil()));
        }
        if (!employee.hasPin() || !encoder.matches(nullToEmpty(currentPin), employee.getPinHash())) {
            employee.recordPinFailure(PinPolicy.MAX_FAIL, PinPolicy.LOCK_FOR);
            throw new IllegalArgumentException(employee.isPinLocked()
                    ? lockMessage(employee.getPinLockedUntil())
                    : "현재 PIN 이 일치하지 않습니다");
        }
        validateNew(employee, newPin, confirmPin);
        employee.assignPin(encoder.encode(newPin), false);
    }

    /** 강제 변경 화면. 이미 PIN 을 맞춰 들어온 상태라 현재 PIN 을 다시 묻지 않는다. */
    @Transactional
    public void changeAfterForcedLogin(String empNo, String newPin, String confirmPin) {
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다"));
        validateNew(employee, newPin, confirmPin);
        employee.assignPin(encoder.encode(newPin), false);
    }

    private void validateNew(Employee employee, String newPin, String confirmPin) {
        if (!PinPolicy.isValidFormat(newPin)) {
            throw new IllegalArgumentException(PinPolicy.formatMessage());
        }
        if (!newPin.equals(confirmPin)) {
            throw new IllegalArgumentException("새 PIN 이 서로 다릅니다");
        }
        // 평문이 아니라 저장된 해시와 견준다. 강제 변경 화면은 현재 PIN 을 받지 않기 때문이다.
        if (employee.hasPin() && encoder.matches(newPin, employee.getPinHash())) {
            throw new IllegalArgumentException("현재 PIN 과 다른 값을 입력해 주세요");
        }
    }

    private static String lockMessage(LocalDateTime until) {
        long minutes = Math.max(1, Duration.between(LocalDateTime.now(), until).toMinutes() + 1);
        return "PIN 을 " + PinPolicy.MAX_FAIL + "회 잘못 입력해 잠겼습니다. "
                + minutes + "분 뒤에 다시 시도해 주세요";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 검증 결과. 실패 사유는 화면에 그대로 보여줄 문장이다. */
    public record Result(boolean accepted, boolean changeRequired, String message) {

        static Result accepted(boolean changeRequired) {
            return new Result(true, changeRequired, null);
        }

        static Result rejected(String message) {
            return new Result(false, false, message);
        }
    }
}
