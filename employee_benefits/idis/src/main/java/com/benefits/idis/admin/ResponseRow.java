package com.benefits.idis.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 응답 목록 한 줄. 응답자만이 아니라 대상 직원 전체가 한 줄씩 나오고,
 * 아직 안 낸 사람은 submittedAt 이 null 이다.
 */
public record ResponseRow(
        String empNo,
        String name,
        Long departmentId,
        String departmentName,
        String typeLabel,
        String phone,
        LocalDate hireDate,
        LocalDateTime submittedAt,
        boolean edited
) {

    public boolean answered() {
        return submittedAt != null;
    }
}
