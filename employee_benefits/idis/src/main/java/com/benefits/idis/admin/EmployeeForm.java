package com.benefits.idis.admin;

import com.benefits.idis.employee.EmployeeType;
import com.benefits.idis.employee.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 직원 추가·수정 모달 입력. 엔티티가 아니라 화면 DTO 라 Setter 를 둔다. */
@Getter
@Setter
@NoArgsConstructor
public class EmployeeForm {

    private String empNo;
    private String name;
    private Long departmentId;
    private EmployeeType type;
    private String phone;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hireDate;

    /** 수정에서만 쓴다. 추가 시에는 항상 EMPLOYEE. */
    private Role role;

    /**
     * 관리자로 올릴 때 줄 초기 PIN, 또는 기존 관리자의 PIN 초기화 값.
     * 슈퍼 관리자만 보내며, 비워 두면 PIN 은 건드리지 않는다.
     * 원문은 서비스에서 해시로 바꾼 뒤 버린다.
     */
    private String initialPin;
}
