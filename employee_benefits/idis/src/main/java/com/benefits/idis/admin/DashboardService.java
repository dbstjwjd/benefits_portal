package com.benefits.idis.admin;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormRepository;
import com.benefits.idis.response.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_LIMIT = 8;
    private static final String NO_DEPARTMENT = "미지정";

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;
    private final EmployeeRepository employeeRepository;
    private final FormAdminService formAdminService;

    public DashboardView load() {
        LocalDate today = LocalDate.now();
        List<Form> forms = formRepository.findAllWithTargets();
        List<Form> open = forms.stream().filter(Form::isOpen).toList();
        List<Employee> actives = employeeRepository.findActiveWithDepartment();

        long dueSoon = open.stream().filter(form -> {
            Long left = form.daysUntilEnd();
            return left != null && left <= FormSummary.DUE_SOON_DAYS;
        }).count();

        YearMonth month = YearMonth.from(today);

        return new DashboardView(
                today,
                open.size(),
                dueSoon,
                responseRepository.countByCreatedAtBetween(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                actives.size(),
                openFormRows(),
                departmentRates(open, actives),
                recent(),
                newcomers(month));
    }

    /** 진행 중인 폼 목록. 폼 관리와 같은 계산을 쓰려고 그 쪽 목록을 그대로 가져온다. */
    private List<FormRow> openFormRows() {
        return formAdminService
                .list(new FormSearch(null, FormSearch.OPEN, 0))
                .rows()
                .getContent();
    }

    /**
     * 부서별 응답률. 진행 중인 폼이 여럿이면 폼마다 대상과 응답을 세어 부서 단위로 합산한다.
     * 한 사람이 두 폼의 대상이면 두 번 세는데, 폼 단위 참여율을 보는 지표라 그게 맞다.
     */
    private List<DashboardView.DeptRate> departmentRates(List<Form> open, List<Employee> actives) {
        Map<String, long[]> byDepartment = new LinkedHashMap<>();

        for (Form form : open) {
            Set<String> responded = new HashSet<>();
            responseRepository.findWithEmployeeByFormId(form.getId())
                    .forEach(response -> responded.add(response.getEmployee().getEmpNo()));

            for (Employee employee : actives) {
                if (!form.includes(employee)) {
                    continue;
                }
                String name = employee.getDepartment() == null
                        ? NO_DEPARTMENT : employee.getDepartment().getName();
                long[] counts = byDepartment.computeIfAbsent(name, key -> new long[2]);
                counts[0]++;
                if (responded.contains(employee.getEmpNo())) {
                    counts[1]++;
                }
            }
        }

        List<DashboardView.DeptRate> rates = new ArrayList<>();
        byDepartment.forEach((name, counts) -> rates.add(new DashboardView.DeptRate(
                name, counts[1], counts[0], percent(counts[1], counts[0]))));

        // 낮은 순으로 두어 손이 필요한 부서가 위에 오게 한다
        rates.sort(Comparator.comparingInt(DashboardView.DeptRate::percent)
                .thenComparing(DashboardView.DeptRate::departmentName));
        return rates;
    }

    private List<DashboardView.Recent> recent() {
        return responseRepository.findRecent(PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(response -> new DashboardView.Recent(
                        response.getEmployee().getName(),
                        response.getForm().getTitle(),
                        response.getCreatedAt()))
                .toList();
    }

    private List<DashboardView.Newcomer> newcomers(YearMonth month) {
        return employeeRepository.findHiredBetween(month.atDay(1), month.atEndOfMonth()).stream()
                .map(employee -> new DashboardView.Newcomer(
                        employee.getName(),
                        employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                        employee.getHireDate()))
                .toList();
    }

    private static int percent(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / whole);
    }
}
