package com.benefits.idis.admin;

import com.benefits.idis.form.QuestionConfig;
import com.benefits.idis.form.QuestionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 질문 입력. 타입별로 쓰는 필드가 달라서 안 쓰는 값은 그냥 비워 둔다. */
@Getter
@Setter
@NoArgsConstructor
public class QuestionForm {

    private QuestionType type;
    private String title;
    private boolean required;

    /** MULTI_CHOICE 전용. 비우면 제한 없음. */
    private Integer maxSelect;

    /** IMAGE_CHOICE 전용. 여러 개 선택 허용 여부. */
    private boolean multiple;

    /** DATE 전용. 둘 다 선택 사항. */
    private String minDate;
    private String maxDate;

    private List<ChoiceForm> choices = new ArrayList<>();

    /** 타입에 해당하는 값만 담아 config 로 만든다. 다른 타입의 값이 섞여 남지 않게 한다. */
    public QuestionConfig toConfig() {
        return switch (type) {
            case MULTI_CHOICE -> new QuestionConfig(maxSelect, null, null, null);
            case IMAGE_CHOICE -> new QuestionConfig(null, multiple ? Boolean.TRUE : null, null, null);
            case DATE -> new QuestionConfig(null, null, blankToNull(minDate), blankToNull(maxDate));
            default -> QuestionConfig.EMPTY;
        };
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
