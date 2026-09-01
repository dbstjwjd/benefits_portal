package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.form.Choice;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormRepository;
import com.benefits.idis.form.Question;
import com.benefits.idis.form.QuestionType;
import com.benefits.idis.response.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormEditService {

    private static final int MAX_TITLE = 200;
    private static final int MAX_QUESTION_TITLE = 500;

    private final FormRepository formRepository;
    private final DepartmentRepository departmentRepository;
    private final ResponseRepository responseRepository;

    public Form load(Long id) {
        return formRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));
    }

    /** 응답이 하나라도 있으면 질문 구조를 건드리지 못하게 잠근다. */
    public boolean locked(Long id) {
        return id != null && responseRepository.existsByFormId(id);
    }

    public long responseCount(Long id) {
        return id == null ? 0 : responseRepository.countByFormId(id);
    }

    @Transactional
    public Long create(FormEditForm input, Employee author) {
        validate(input);

        Form form = Form.builder()
                .title(input.getTitle().strip())
                .description(blankToNull(input.getDescription()))
                .target(input.getTarget())
                .endAt(input.endAt())
                .createdBy(author)
                .build();
        form.open();
        form.replaceTargetDepartments(departments(input));
        form.replaceQuestions(buildQuestions(input));

        return formRepository.save(form).getId();
    }

    @Transactional
    public void update(Long id, FormEditForm input) {
        Form form = load(id);

        // 응답이 쌓인 뒤에는 마감일만 손댈 수 있다. 나머지 입력은 들어와도 무시한다.
        if (locked(id)) {
            form.changeEndAt(input.endAt());
            return;
        }

        validate(input);
        form.update(input.getTitle().strip(), blankToNull(input.getDescription()),
                input.getTarget(), form.getStartAt(), input.endAt());
        form.replaceTargetDepartments(departments(input));
        form.replaceQuestions(buildQuestions(input));
    }

    private List<Department> departments(FormEditForm input) {
        if (input.getDepartmentIds() == null || input.getDepartmentIds().isEmpty()) {
            return List.of();
        }
        List<Department> found = departmentRepository.findAllById(input.getDepartmentIds());
        if (found.size() != input.getDepartmentIds().size()) {
            throw new IllegalArgumentException("없는 부서가 대상에 들어 있습니다");
        }
        return found;
    }

    private static List<Question> buildQuestions(FormEditForm input) {
        List<Question> questions = new ArrayList<>();
        int order = 1;
        for (QuestionForm source : input.getQuestions()) {
            Question question = Question.builder()
                    .type(source.getType())
                    .title(source.getTitle().strip())
                    .required(source.isRequired())
                    .sortOrder(order++)
                    .config(source.toConfig().toJson())
                    .build();

            if (source.getType().hasChoices()) {
                int choiceOrder = 1;
                for (ChoiceForm choice : source.getChoices()) {
                    question.addChoice(Choice.builder()
                            .content(choice.getContent().strip())
                            .imagePath(blankToNull(choice.getImagePath()))
                            .sortOrder(choiceOrder++)
                            .build());
                }
            }
            questions.add(question);
        }
        return questions;
    }

    // ── 검증 ────────────────────────────────────────────────

    private static void validate(FormEditForm input) {
        String title = blankToNull(input.getTitle());
        if (title == null) {
            throw new IllegalArgumentException("폼 제목을 입력해주세요");
        }
        if (title.length() > MAX_TITLE) {
            throw new IllegalArgumentException("폼 제목은 " + MAX_TITLE + "자까지 넣을 수 있습니다");
        }
        if (input.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("질문을 하나 이상 추가해주세요");
        }

        int number = 1;
        for (QuestionForm question : input.getQuestions()) {
            validateQuestion(question, number++);
        }
    }

    private static void validateQuestion(QuestionForm question, int number) {
        String label = "질문 " + number;
        if (question.getType() == null) {
            throw new IllegalArgumentException(label + " 의 타입을 골라주세요");
        }
        String title = blankToNull(question.getTitle());
        if (title == null) {
            throw new IllegalArgumentException(label + " 의 내용을 입력해주세요");
        }
        if (title.length() > MAX_QUESTION_TITLE) {
            throw new IllegalArgumentException(label + " 의 내용이 너무 깁니다");
        }

        if (question.getType().hasChoices()) {
            validateChoices(question, label);
        }
        if (question.getType() == QuestionType.DATE) {
            validateDateRange(question, label);
        }
    }

    private static void validateChoices(QuestionForm question, String label) {
        List<ChoiceForm> choices = question.getChoices();
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(label + " 의 선택지를 하나 이상 추가해주세요");
        }
        for (ChoiceForm choice : choices) {
            if (blankToNull(choice.getContent()) == null) {
                throw new IllegalArgumentException(label + " 에 내용이 비어 있는 선택지가 있습니다");
            }
            if (question.getType() == QuestionType.IMAGE_CHOICE
                    && blankToNull(choice.getImagePath()) == null) {
                throw new IllegalArgumentException(label + " 의 선택지마다 이미지를 올려주세요");
            }
        }
        if (question.getType() == QuestionType.MULTI_CHOICE && question.getMaxSelect() != null) {
            int max = question.getMaxSelect();
            if (max < 1 || max > choices.size()) {
                throw new IllegalArgumentException(
                        label + " 의 최대 선택 수는 1 이상 " + choices.size() + " 이하여야 합니다");
            }
        }
    }

    private static void validateDateRange(QuestionForm question, String label) {
        LocalDate from = parseDate(question.getMinDate(), label);
        LocalDate to = parseDate(question.getMaxDate(), label);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(label + " 의 허용 범위가 뒤집혀 있습니다");
        }
    }

    private static LocalDate parseDate(String value, String label) {
        if (blankToNull(value) == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " 의 날짜 형식이 올바르지 않습니다");
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
