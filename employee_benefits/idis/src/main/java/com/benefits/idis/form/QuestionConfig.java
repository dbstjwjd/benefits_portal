package com.benefits.idis.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 질문 타입마다 다른 부가 설정. {@link Question#getConfig()} 에 JSON 으로 저장한다.
 * 쓰지 않는 값은 null 이고, 타입별로 쓰는 키는 아래가 전부다.
 *
 * <pre>
 *   MULTI_CHOICE   {"maxSelect": 3}                              // 없으면 제한 없음
 *   IMAGE_CHOICE   {"multiple": true}                            // 없으면 하나만 선택
 *   DATE           {"minDate": "2026-09-01", "maxDate": "..."}   // 둘 다 선택 사항
 *   그 밖의 타입    설정 없음(null)
 * </pre>
 *
 * 날짜를 문자열로 두는 것은 date 입력의 min/max 속성에 그대로 넣기 위해서다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionConfig(Integer maxSelect, Boolean multiple, String minDate, String maxDate) {

    public static final QuestionConfig EMPTY = new QuestionConfig(null, null, null, null);

    // 나중에 키가 늘어도 예전 폼이 깨지지 않도록 모르는 키는 무시한다
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** 깨진 값이 들어 있어도 화면이 죽지 않도록 빈 설정으로 되돌린다. */
    public static QuestionConfig parse(String json) {
        if (json == null || json.isBlank()) {
            return EMPTY;
        }
        try {
            return MAPPER.readValue(json, QuestionConfig.class);
        } catch (JacksonException e) {
            return EMPTY;
        }
    }

    /** 저장할 JSON. 설정이 하나도 없으면 컬럼을 비운다. */
    public String toJson() {
        return isEmpty() ? null : MAPPER.writeValueAsString(this);
    }

    /** is 로 시작해 두면 Jackson 이 empty 키로 같이 저장해 버려서 제외한다. */
    @JsonIgnore
    public boolean isEmpty() {
        return maxSelect == null && multiple == null && minDate == null && maxDate == null;
    }

    /** 이미지 선택에서 여러 개를 고를 수 있는지. */
    public boolean multiSelectImage() {
        return Boolean.TRUE.equals(multiple);
    }
}
