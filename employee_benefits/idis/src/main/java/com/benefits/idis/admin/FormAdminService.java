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
        // 요약 카드는 살아 있는 폼 기준이라 삭제됨 탭에서도 같은 값을 쓴다
        List<Form> forms = formRepository.findAllWithTargets();
        List<Form> source = search.deletedTab() ? formRepository.findDeletedWithTargets() : forms;

        List<Form> matched = source.stream()
                .filter(form -> matchesKeyword(form, search.keyword()))
                .filter(form -> search.deletedTab() || matchesStatus(form, search.status()))
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
                actives.stream().filter(form::includes).count(),
                form.isDeleted());
    }

    /**
     * 폼 삭제.
     * 응답이 있으면 지우지 않고 감춘다(soft). 지워 버리면 그 응답이 어느 폼의 것인지 알 수 없다.
     * 응답이 0건이면 남길 것이 없으니 완전히 지운다. 잘못 만든 폼을 치우는 용도다.
     */
    @Transactional
    public DeleteResult delete(Long id) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));
        long responses = responseRepository.countByFormId(id);
        if (responses > 0) {
            form.softDelete();
            return new DeleteResult(false, responses);
        }
        // 질문·선택지는 cascade 로 함께 지워진다. 응답이 없으니 남는 참조도 없다.
        formRepository.delete(form);
        return new DeleteResult(true, 0);
    }

    @Transactional
    public void restore(Long id) {
        Form form = formRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));
        if (!form.isDeleted()) {
            throw new IllegalArgumentException("삭제된 폼이 아닙니다");
        }
        form.restore();
    }

    /** 어느 쪽으로 지웠는지. 안내 문구를 고르는 데 쓴다. */
    public record DeleteResult(boolean removed, long keptResponses) {
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
