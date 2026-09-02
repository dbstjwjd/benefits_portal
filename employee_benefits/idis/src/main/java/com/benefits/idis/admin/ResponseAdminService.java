package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.employee.EmployeeType;
import com.benefits.idis.form.Choice;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormRepository;
import com.benefits.idis.form.FormTarget;
import com.benefits.idis.form.Question;
import com.benefits.idis.response.Answer;
import com.benefits.idis.response.Response;
import com.benefits.idis.response.ResponseRepository;
import com.benefits.idis.response.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponseAdminService {

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;
    private final EmployeeRepository employeeRepository;
    private final ResponseService responseService;

    /** 폼 선택 드롭다운. 진행 중을 먼저 두고 마감이 가까운 순으로, 끝난 폼은 최근 마감 순으로 본다. */
    /** 엑셀 모달에 뿌릴 질문 목록. 폼에 저장된 순서 그대로다. */
    public List<ExportQuestion> exportQuestions(Long formId) {
        return formRepository.findByIdAndDeletedAtIsNull(formId)
                .map(form -> form.getQuestions().stream()
                        .map(q -> new ExportQuestion(q.getId(), q.getTitle()))
                        .toList())
                .orElseGet(List::of);
    }

    public record ExportQuestion(Long id, String title) {
    }

    public List<FormOption> options() {
        Map<Long, Long> counts = responseCounts();
        List<Employee> actives = employeeRepository.findActiveWithDepartment();

        return formRepository.findAllWithTargets().stream()
                .sorted(byOpenThenDeadline())
                .map(form -> {
                    long target = actives.stream().filter(form::includes).count();
                    long answered = counts.getOrDefault(form.getId(), 0L);
                    return new FormOption(form.getId(), form.getTitle(),
                            percent(answered, Math.max(target, answered)), form.isOpen());
                })
                .toList();
    }

    /** 처음 들어왔을 때 열어 줄 폼. 진행 중 가운데 마감이 가장 가까운 것을 고른다. */
    public Long defaultFormId() {
        return formRepository.findAllWithTargets().stream()
                .sorted(byOpenThenDeadline())
                .map(Form::getId)
                .findFirst()
                .orElse(null);
    }

    public ResponseListView load(Long formId, ResponseSearch search) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(formId)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));

        List<Response> responses = responseRepository.findWithEmployeeByFormId(formId);
        Map<String, Response> byEmpNo = new HashMap<>();
        responses.forEach(response -> byEmpNo.put(response.getEmployee().getEmpNo(), response));

        List<Employee> targets = targets(form, responses);
        List<ResponseRow> all = targets.stream()
                .map(employee -> toRow(employee, byEmpNo.get(employee.getEmpNo())))
                .toList();

        ResponseOverview overview = new ResponseOverview(
                form.getId(), form.getTitle(), form.getEndAt(), form.daysUntilEnd(), form.isClosed(),
                typeLabel(form.getTarget()),
                form.getTargetDepartments().stream().map(Department::getName).sorted().toList(),
                all.stream().filter(ResponseRow::answered).count(),
                all.size());

        List<ResponseRow> matched = all.stream()
                .filter(row -> matchesKeyword(row, search.keyword()))
                .filter(row -> matchesDepartment(row, search.departmentId()))
                .filter(row -> matchesStatus(row, search.status()))
                .toList();

        int from = Math.min(search.page() * ResponseSearch.PAGE_SIZE, matched.size());
        int to = Math.min(from + ResponseSearch.PAGE_SIZE, matched.size());
        Page<ResponseRow> page = new PageImpl<>(matched.subList(from, to),
                PageRequest.of(search.page(), ResponseSearch.PAGE_SIZE), matched.size());

        return new ResponseListView(overview, page,
                search.statsTab() ? stats(form, responses.size()) : List.of());
    }

    /** 목록의 눈 아이콘으로 여는 개별 응답. */
    public ResponseDetail detail(Long formId, String empNo) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(formId)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));
        Response response = responseRepository.findByFormIdAndEmployeeEmpNo(formId, empNo)
                .orElseThrow(() -> new IllegalArgumentException("응답을 찾을 수 없습니다"));

        Map<Long, Answer> byQuestion = new HashMap<>();
        response.getAnswers().forEach(answer -> byQuestion.put(answer.getQuestion().getId(), answer));

        List<ResponseDetail.Item> items = form.getQuestions().stream()
                .map(question -> {
                    String text = responseService.describe(byQuestion.get(question.getId()));
                    return new ResponseDetail.Item(question.getTitle(), text.isBlank() ? "-" : text);
                })
                .toList();

        Employee employee = response.getEmployee();
        return new ResponseDetail(employee.getName(),
                employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                response.getCreatedAt(), response.getEditedAt() != null, items);
    }

    // ── 내부 ────────────────────────────────────────────────

    /**
     * 목록에 올릴 사람. 지금 대상인 재직자에 더해, 대상에서 빠졌거나 퇴사했어도
     * 이미 응답을 남긴 사람을 넣는다. 그래야 응답 수와 목록이 어긋나지 않는다.
     */
    private List<Employee> targets(Form form, List<Response> responses) {
        Map<String, Employee> people = new LinkedHashMap<>();
        employeeRepository.findActiveWithDepartment().stream()
                .filter(form::includes)
                .forEach(employee -> people.put(employee.getEmpNo(), employee));
        responses.forEach(response ->
                people.putIfAbsent(response.getEmployee().getEmpNo(), response.getEmployee()));

        return people.values().stream()
                .sorted(Comparator.comparing(Employee::getName).thenComparing(Employee::getEmpNo))
                .toList();
    }

    private static ResponseRow toRow(Employee employee, Response response) {
        return new ResponseRow(
                employee.getEmpNo(),
                employee.getName(),
                employee.getDepartment() == null ? null : employee.getDepartment().getId(),
                employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                employee.getType() == EmployeeType.DIRECT ? "직접직" : "간접직",
                employee.getPhone(),
                employee.getHireDate(),
                response == null ? null : response.getCreatedAt(),
                response != null && response.getEditedAt() != null);
    }

    /** 선택지별 집계. 퍼센트 분모는 그 폼에 응답한 사람 수다. */
    private List<QuestionStat> stats(Form form, int respondents) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : responseRepository.countChoicesByFormId(form.getId())) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        List<QuestionStat> stats = new ArrayList<>();
        int number = 1;
        for (Question question : form.getQuestions()) {
            List<QuestionStat.Bar> bars = List.of();
            if (question.getType().hasChoices()) {
                bars = question.getChoices().stream()
                        .map(choice -> toBar(choice, counts.getOrDefault(choice.getId(), 0L), respondents))
                        .toList();
            }
            stats.add(new QuestionStat(number++, question.getTitle(),
                    question.getType().label(), bars.isEmpty(), bars));
        }
        return stats;
    }

    private static QuestionStat.Bar toBar(Choice choice, long count, int respondents) {
        return new QuestionStat.Bar(choice.getContent(), choice.getImagePath(),
                count, percent(count, respondents));
    }

    private Map<Long, Long> responseCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : responseRepository.countGroupedByFormId()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    /** 엑셀은 화면에 걸린 필터를 그대로 따르되 페이지는 무시하고 전부 내려준다. */
    public List<ResponseRow> rowsForExport(Long formId, ResponseSearch search) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(formId)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));
        List<Response> responses = responseRepository.findWithEmployeeByFormId(formId);
        Map<String, Response> byEmpNo = new HashMap<>();
        responses.forEach(response -> byEmpNo.put(response.getEmployee().getEmpNo(), response));

        List<Employee> targets = targets(form, responses);
        return targets.stream()
                .map(employee -> toRow(employee, byEmpNo.get(employee.getEmpNo())))
                .filter(row -> matchesKeyword(row, search.keyword()))
                .filter(row -> matchesDepartment(row, search.departmentId()))
                .filter(row -> matchesStatus(row, search.status()))
                .toList();
    }

    private static Comparator<Form> byOpenThenDeadline() {
        return (a, b) -> {
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
    }

    private static boolean matchesKeyword(ResponseRow row, String keyword) {
        return keyword == null || row.name().contains(keyword);
    }

    private static boolean matchesDepartment(ResponseRow row, Long departmentId) {
        return departmentId == null || departmentId.equals(row.departmentId());
    }

    private static boolean matchesStatus(ResponseRow row, String status) {
        return switch (status) {
            case ResponseSearch.ANSWERED -> row.answered();
            case ResponseSearch.PENDING -> !row.answered();
            default -> true;
        };
    }

    private static String typeLabel(FormTarget target) {
        return switch (target) {
            case DIRECT -> "직접직";
            case INDIRECT -> "간접직";
            case ALL -> null;
        };
    }

    private static int percent(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return (int) Math.round(part * 100.0 / whole);
    }
}
