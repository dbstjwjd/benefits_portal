package com.benefits.idis.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 대시보드 한 화면. */
public record DashboardView(
        LocalDate today,
        long openFormCount,
        long dueSoonCount,
        long todayResponseCount,
        long activeCount,
        List<FormRow> openForms,
        List<DeptRate> departmentRates,
        List<Recent> recentResponses,
        List<Newcomer> newcomers
) {

    /** 부서별 응답률. 진행 중인 폼이 여럿이면 대상·응답을 모두 합산한 값이다. */
    public record DeptRate(String departmentName, long answered, long targetCount, int percent) {
    }

    public record Recent(String name, String formTitle, LocalDateTime at) {
    }

    public record Newcomer(String name, String departmentName, LocalDate hireDate) {
    }
}
