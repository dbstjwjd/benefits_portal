package com.benefits.idis.response;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.form.Choice;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public boolean hasSubmitted(Long formId, String empNo) {
        return responseRepository.findByFormIdAndEmployeeEmpNo(formId, empNo).isPresent();
    }

    /**
     * 신청 내역 화면용 조회. 마감되었거나 더 이상 대상이 아니게 된 폼도
     * 신청 이력에서는 계속 보여야 하므로 /forms 목록과 달리 상태 필터를 두지 않는다.
     */
    public List<MyResponseView> findMyResponses(String empNo) {
        return responseRepository.findWithFormByEmpNo(empNo).stream()
                .map(response -> new MyResponseView(
                        response.getForm().getId(),
                        response.getForm().getTitle(),
                        response.getForm().isOpen(),
                        response.getForm().getEndAt(),
                        response.getCreatedAt(),
                        response.getEditedAt() != null))
                .toList();
    }

    /**
     * 저장된 응답을 화면 입력값(input name → value) 형태로 펼친다.
     * 최초 작성이면 빈 map 이고, 화면은 이 map 하나만 보고 값을 채운다.
     */
    public Map<String, String> loadValues(Long formId, String empNo) {
        Map<String, String> values = new HashMap<>();
        responseRepository.findByFormIdAndEmployeeEmpNo(formId, empNo).ifPresent(response -> {
            for (Answer answer : response.getAnswers()) {
                String key = "q" + answer.getQuestion().getId();
                switch (answer.getQuestion().getType()) {
                    case ADDRESS -> readAddress(answer.getValue())
                            .forEach((suffix, value) -> values.put(key + suffix, value));
                    case SINGLE_CHOICE, DROPDOWN, IMAGE_CHOICE -> answer.getAnswerChoices().stream()
                            .findFirst()
                            .ifPresent(chosen -> values.put(key, String.valueOf(chosen.getChoice().getId())));
                    default -> values.put(key, answer.getValue());
                }
            }
        });
        return values;
    }

    /**
     * 필수 항목 누락을 저장 트랜잭션 밖에서 먼저 걸러낸다.
     * 트랜잭션 안에서 예외로 롤백시키면 그 뒤 화면을 다시 그릴 때
     * 지연 로딩이 깨져서(LazyInitializationException) 에러 화면을 보여줄 수 없다.
     */
    public List<String> validate(Form form, MultiValueMap<String, String> params) {
        List<String> errors = new ArrayList<>();
        for (Question question : form.getQuestions()) {
            if (!question.getType().isAnswerable() || !question.isRequired()) {
                continue;
            }
            // 저장과 같은 함수로 판단해야 "검증은 통과했는데 저장은 비어있는" 불일치가 생기지 않는다.
            if (buildAnswer(question, params).isEmpty()) {
                errors.add("'" + question.getTitle() + "' 항목을 입력해주세요");
            }
        }
        return errors;
    }

    /**
     * 신규 제출과 수정을 같이 처리한다.
     * (form_id, emp_no) 유니크 제약이 있어 1인 1응답이므로, 기존 응답이 있으면 답변만 갈아끼운다.
     * 호출 전에 {@link #validate} 로 필수 항목을 확인한다.
     */
    @Transactional
    public void submit(Form form, Employee employee, MultiValueMap<String, String> params) {
        Response response = responseRepository.findByFormIdAndEmployeeEmpNo(form.getId(), employee.getEmpNo())
                .map(existing -> {
                    existing.markEdited();
                    return existing;
                })
                .orElseGet(() -> responseRepository.save(
                        Response.builder().form(form).employee(employee).build()));
        response.clearAnswers();

        for (Question question : form.getQuestions()) {
            if (!question.getType().isAnswerable()) {
                continue;
            }
            Answer answer = buildAnswer(question, params);
            if (!answer.isEmpty()) {
                response.addAnswer(answer);
            }
        }
        // response 는 영속 상태이므로 커밋 시점에 답변 변경까지 함께 flush 된다. save() 재호출은 불필요.
    }

    private Answer buildAnswer(Question question, MultiValueMap<String, String> params) {
        String key = "q" + question.getId();
        String value = null;
        Choice choice = null;

        switch (question.getType()) {
            case ADDRESS -> {
                String zipcode = trimmed(params.getFirst(key + "_zipcode"));
                String address = trimmed(params.getFirst(key + "_address"));
                String detail = trimmed(params.getFirst(key + "_detail"));
                if (!zipcode.isEmpty() || !address.isEmpty()) {
                    value = writeAddress(zipcode, address, detail);
                }
            }
            case SINGLE_CHOICE, DROPDOWN, IMAGE_CHOICE -> choice = resolveChoice(question, trimmed(params.getFirst(key)));
            default -> {
                String text = trimmed(params.getFirst(key));
                if (!text.isEmpty()) {
                    value = text;
                }
            }
        }

        Answer answer = Answer.builder().question(question).value(value).build();
        if (choice != null) {
            answer.addChoice(AnswerChoice.builder().choice(choice).build());
        }
        return answer;
    }

    /**
     * 해당 질문에 달린 선택지에서만 찾는다. 남의 질문 선택지나 없는 id 가 넘어오면 null 이고,
     * 그러면 답변이 비어 있는 것으로 취급되어 필수 항목이면 검증에서 걸린다.
     */
    private Choice resolveChoice(Question question, String choiceId) {
        if (choiceId.isEmpty()) {
            return null;
        }
        return question.getChoices().stream()
                .filter(candidate -> String.valueOf(candidate.getId()).equals(choiceId))
                .findFirst()
                .orElse(null);
    }

    /** 배송업체 전달용 엑셀에서 우편번호를 따로 뽑을 수 있도록 구조를 유지해 저장한다. */
    private String writeAddress(String zipcode, String address, String detail) {
        return objectMapper.writeValueAsString(
                Map.of("zipcode", zipcode, "address", address, "detail", detail));
    }

    private Map<String, String> readAddress(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, new TypeReference<>() {
            });
            return Map.of(
                    "_zipcode", parsed.getOrDefault("zipcode", ""),
                    "_address", parsed.getOrDefault("address", ""),
                    "_detail", parsed.getOrDefault("detail", ""));
        } catch (JacksonException e) {
            return Map.of();
        }
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.strip();
    }
}
