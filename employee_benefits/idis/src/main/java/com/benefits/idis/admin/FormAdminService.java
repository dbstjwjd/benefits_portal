package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormRepository;
import com.benefits.idis.form.FormTarget;
import com.benefits.idis.response.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormAdminService {

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * 진행 중을 위에 두고 마감이 가까운 순으로, 끝난 폼은 최근에 마감된 순으로 본다.
     * 마감일이 없는 폼은 급할 게 없으니 뒤로 보낸다.
     */
    private static final Comparator<Form> LIST_ORDER = (a, b) -> {
        int group = Boolean.compare(!a.isOpen(), !b.isOpen());
        if (group != 0) {
            return group;
        }
        LocalDateTime x = a.getEndAt();
        LocalDateTime y = b.getEndAt();
        if (x == null || y == null) {
            return x == null ? (y == null ? 0 : 1) : -1;
        }
        return a.isOpen() ? x.compareTo(y) : y.compareTo(x);
    };

    public FormListView list(FormSearch search) {
        List<Form> forms = formRepository.findAllWithTargets();

        List<Form> matched = forms.stream()
                .filter(form -> matchesKeyword(form, search.keyword()))
                .filter(form -> matchesStatus(form, search.status()))
                .sorted(LIST_ORDER)
                .toList();

        int from = Math.min(search.page() * FormSearch.PAGE_SIZE, matched.size());
        int to = Math.min(from + FormSearch.PAGE_SIZE, matched.size());

        // 대상 인원은 폼마다 다르지만 판정 대상은 같은 재직자 목록이라 한 번만 읽는다
        List<Employee> actives = employeeRepository.findActiveWithDepartment();
        List<FormRow> rows = matched.subList(from, to).stream()
                .map(form -> toRow(form, actives))
                .toList();

        Page<FormRow> page = new PageImpl<>(rows,
                PageRequest.of(search.page(), FormSearch.PAGE_SIZE), matched.size());
        return new FormListView(page, summary(forms));
    }

    private FormRow toRow(Form form, List<Employee> actives) {
        return new FormRow(
                form.getId(),
                form.getTitle(),
                typeLabel(form.getTarget()),
                form.getTargetDepartments().stream().map(Department::getName).sorted().toList(),
                form.getEndAt(),
                form.daysUntilEnd(),
                form.isOpen(),
                form.isClosed(),
                responseRepository.countByFormId(form.getId()),
                actives.stream().filter(form::includes).count());
    }

    private static FormSummary summary(List<Form> forms) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonth = monthStart.plusMonths(1);

        long open = forms.stream().filter(Form::isOpen).count();
        long dueSoon = forms.stream().filter(Form::isOpen).filter(form -> {
            Long left = form.daysUntilEnd();
            return left != null && left <= FormSummary.DUE_SOON_DAYS;
        }).count();
        long endingThisMonth = forms.stream()
                .filter(form -> form.getEndAt() != null)
                .filter(form -> !form.getEndAt().isBefore(monthStart)
                        && form.getEndAt().isBefore(nextMonth))
                .count();

        return new FormSummary(forms.size(), open, dueSoon, endingThisMonth);
    }

    private static boolean matchesKeyword(Form form, String keyword) {
        return keyword == null || form.getTitle().toLowerCase().contains(keyword.toLowerCase());
    }

    private static boolean matchesStatus(Form form, String status) {
        return switch (status) {
            case FormSearch.CLOSED -> form.isClosed();
            case FormSearch.ALL -> true;
            default -> form.isOpen();
        };
    }

    /** 전체 대상이면 대상 칸에 구분 배지를 그리지 않는다. */
    private static String typeLabel(FormTarget target) {
        return switch (target) {
            case DIRECT -> "직접직";
            case INDIRECT -> "간접직";
            case ALL -> null;
        };
    }
}
