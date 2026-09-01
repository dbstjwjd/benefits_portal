package com.benefits.idis.response;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.form.Choice;
import com.benefits.idis.form.Form;
import com.benefits.idis.form.Question;
import com.benefits.idis.form.QuestionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
     * 신청내역 화면용 조회. 마감되었거나 더 이상 대상이 아니게 된 폼도
     * 신청 이력에서는 계속 보여야 하므로 /forms 목록과 달리 상태 필터를 두지 않는다.
     */
    public List<MyResponseView> findMyResponses(String empNo) {
        return responseRepository.findWithFormByEmpNo(empNo).stream()
                .map(response -> new MyResponseView(
                        response.getForm().getId(),
                        response.getForm().getTitle(),
                        response.getForm().isOpen(),
                        response.getCreatedAt(),
                        summarize(response)))
                .toList();
    }

    /** 카드에 한 줄로 보여줄 대표 응답. 선택지 답변을 우선하고, 없으면 첫 텍스트 답변을 쓴다. */
    private String summarize(Response response) {
        String fallback = null;
        for (Answer answer : response.getAnswers()) {
            if (!answer.getAnswerChoices().isEmpty()) {
                return answer.getAnswerChoices().stream()
                        .map(chosen -> chosen.getChoice().getContent())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");
            }
            if (fallback == null && answer.getValue() != null && !answer.getValue().isBlank()
                    && answer.getQuestion().getType() != com.benefits.idis.form.QuestionType.ADDRESS) {
                fallback = answer.getValue();
            }
        }
        return fallback != null ? fallback : "-";
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
                    case SINGLE_CHOICE -> putSingle(values, key, answer);
                    // 화면에서 ",3," 포함 여부로 체크 상태를 판단하므로 앞뒤에도 구분자를 둔다.
                    case MULTI_CHOICE -> putMany(values, key, answer);
                    // 이미지 선택은 여러 개 고를 수 있게 설정할 수 있어 저장 형태가 갈린다.
                    case IMAGE_CHOICE -> {
                        if (answer.getQuestion().configOrEmpty().multiSelectImage()) {
                            putMany(values, key, answer);
                        } else {
                            putSingle(values, key, answer);
                        }
                    }
                    default -> values.put(key, answer.getValue());
                }
            }
        });
        return values;
    }

    private static void putSingle(Map<String, String> values, String key, Answer answer) {
        answer.getAnswerChoices().stream().findFirst()
                .ifPresent(chosen -> values.put(key, String.valueOf(chosen.getChoice().getId())));
    }

    private static void putMany(Map<String, String> values, String key, Answer answer) {
        values.put(key, answer.getAnswerChoices().stream()
                .map(chosen -> String.valueOf(chosen.getChoice().getId()))
                .reduce(",", (acc, id) -> acc + id + ","));
    }

    /**
     * 필수 항목 누락을 저장 트랜잭션 밖에서 먼저 걸러낸다.
     * 트랜잭션 안에서 예외로 롤백시키면 그 뒤 화면을 다시 그릴 때
     * 지연 로딩이 깨져서(LazyInitializationException) 에러 화면을 보여줄 수 없다.
     */
    public List<String> validate(Form form, MultiValueMap<String, String> params) {
        List<String> errors = new ArrayList<>();
        for (Question question : form.getQuestions()) {
            // 저장과 같은 함수로 판단해야 "검증은 통과했는데 저장은 비어있는" 불일치가 생기지 않는다.
            Answer answer = buildAnswer(question, params);
            if (answer.isEmpty()) {
                if (question.isRequired()) {
                    errors.add("'" + question.getTitle() + "' 항목을 입력해주세요");
                }
                continue;
            }
            checkLimits(question, answer, errors);
        }
        return errors;
    }

    /**
     * 관리자가 질문에 걸어 둔 제한(최대 선택 수, 날짜 허용 범위)을 본다.
     * 화면에서도 막지만 폼을 직접 만들어 보내는 경우가 있어 서버에서 한 번 더 확인한다.
     */
    private static void checkLimits(Question question, Answer answer, List<String> errors) {
        QuestionConfig config = question.configOrEmpty();
        String label = "'" + question.getTitle() + "'";

        switch (question.getType()) {
            case MULTI_CHOICE, IMAGE_CHOICE -> {
                Integer max = config.maxSelect();
                if (max != null && answer.getAnswerChoices().size() > max) {
                    errors.add(label + " 은 최대 " + max + "개까지 선택할 수 있습니다");
                }
            }
            case PHONE -> {
                if (formatPhone(answer.getValue()) == null) {
                    errors.add(label + " 의 전화번호 형식이 올바르지 않습니다");
                }
            }
            case DATE -> {
                LocalDate picked = parseDate(answer.getValue());
                if (picked == null) {
                    errors.add(label + " 의 날짜 형식이 올바르지 않습니다");
                    return;
                }
                LocalDate from = parseDate(config.minDate());
                LocalDate to = parseDate(config.maxDate());
                if ((from != null && picked.isBefore(from)) || (to != null && picked.isAfter(to))) {
                    errors.add(label + " 은 " + rangeText(from, to) + " 에서 골라주세요");
                }
            }
            default -> {
            }
        }
    }

    private static String rangeText(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return from + " ~ " + to;
        }
        return from != null ? from + " 이후" : to + " 이전";
    }

    /**
     * 전화번호를 직원 정보와 같은 하이픈 형식으로 맞춘다.
     * 숫자만 남겨 01 로 시작하는 10~11 자리일 때만 값을 주고, 아니면 null 이다.
     */
    private static String formatPhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!digits.startsWith("01") || digits.length() < 10 || digits.length() > 11) {
            return null;
        }
        int middle = digits.length() - 7;
        return digits.substring(0, 3) + "-"
                + digits.substring(3, 3 + middle) + "-"
                + digits.substring(3 + middle);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
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
        List<Choice> chosen = new ArrayList<>();

        switch (question.getType()) {
            case ADDRESS -> {
                String zipcode = trimmed(params.getFirst(key + "_zipcode"));
                String address = trimmed(params.getFirst(key + "_address"));
                String detail = trimmed(params.getFirst(key + "_detail"));
                if (!zipcode.isEmpty() || !address.isEmpty()) {
                    value = writeAddress(zipcode, address, detail);
                }
            }
            case PHONE -> {
                String text = trimmed(params.getFirst(key));
                if (!text.isEmpty()) {
                    // 형식이 맞으면 직원 전화번호와 같은 모양으로 맞춰 저장하고,
                    // 아니면 적은 그대로 두어 checkLimits 에서 잡는다
                    String phone = formatPhone(text);
                    value = phone != null ? phone : text;
                }
            }
            case SINGLE_CHOICE -> pickOne(question, params, key, chosen);
            case MULTI_CHOICE -> pickMany(question, params, key, chosen);
            case IMAGE_CHOICE -> {
                if (question.configOrEmpty().multiSelectImage()) {
                    pickMany(question, params, key, chosen);
                } else {
                    pickOne(question, params, key, chosen);
                }
            }
            default -> {
                String text = trimmed(params.getFirst(key));
                if (!text.isEmpty()) {
                    value = text;
                }
            }
        }

        Answer answer = Answer.builder().question(question).value(value).build();
        for (Choice choice : chosen) {
            answer.addChoice(AnswerChoice.builder().choice(choice).build());
        }
        return answer;
    }

    private void pickOne(Question question, MultiValueMap<String, String> params,
                         String key, List<Choice> chosen) {
        Choice choice = resolveChoice(question, trimmed(params.getFirst(key)));
        if (choice != null) {
            chosen.add(choice);
        }
    }

    private void pickMany(Question question, MultiValueMap<String, String> params,
                          String key, List<Choice> chosen) {
        for (String raw : params.getOrDefault(key, List.of())) {
            Choice choice = resolveChoice(question, trimmed(raw));
            if (choice != null && !chosen.contains(choice)) {
                chosen.add(choice);
            }
        }
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

    /**
     * 답변을 한 줄 문자열로 만든다. 관리자 응답 상세와 엑셀이 같이 쓴다.
     * 주소 파싱이 여기 하나만 있어야 화면과 엑셀이 다르게 보이지 않는다.
     */
    public String describe(Answer answer) {
        if (answer == null) {
            return "";
        }
        if (!answer.getAnswerChoices().isEmpty()) {
            return answer.getAnswerChoices().stream()
                    .map(chosen -> chosen.getChoice().getContent())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
        if (answer.getQuestion().getType() == com.benefits.idis.form.QuestionType.ADDRESS) {
            // 시안의 응답 상세는 우편번호·기본주소·상세주소를 줄을 나눠 보여준다
            return addressParts(answer).stream()
                    .filter(part -> !part.isBlank())
                    .reduce((a, b) -> a + System.lineSeparator() + b)
                    .orElse("");
        }
        return answer.getValue() == null ? "" : answer.getValue();
    }

    /** 주소를 우편번호 / 기본주소 / 상세주소 세 칸으로 준다. 엑셀에서 칸을 나눠 쓴다. */
    public List<String> addressParts(Answer answer) {
        Map<String, String> parsed = answer == null ? Map.of() : readAddress(answer.getValue());
        return List.of(
                parsed.getOrDefault("_zipcode", ""),
                parsed.getOrDefault("_address", ""),
                parsed.getOrDefault("_detail", ""));
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
