package com.benefits.idis.admin;

import com.benefits.idis.form.Question;
import com.benefits.idis.form.QuestionConfig;

import java.util.List;

/**
 * 편집 화면이 읽는 질문 한 개. 카드 마크업을 자바스크립트가 만들기 때문에
 * 저장된 폼을 열 때와 검증에 걸려 되돌아올 때 모두 이 형태로 넘긴다.
 */
public record QuestionView(
        String type,
        String title,
        boolean required,
        Integer maxSelect,
        boolean multiple,
        String minDate,
        String maxDate,
        List<ChoiceView> choices
) {

    public record ChoiceView(String content, String imagePath) {
    }

    public static QuestionView of(Question question) {
        QuestionConfig config = question.configOrEmpty();
        return new QuestionView(
                question.getType().name(),
                question.getTitle(),
                question.isRequired(),
                config.maxSelect(),
                config.multiSelectImage(),
                config.minDate(),
                config.maxDate(),
                question.getChoices().stream()
                        .map(choice -> new ChoiceView(choice.getContent(), choice.getImagePath()))
                        .toList());
    }

    /** 검증에 걸렸을 때 방금 입력한 내용을 그대로 다시 그리려고 쓴다. */
    public static QuestionView of(QuestionForm form) {
        return new QuestionView(
                form.getType() == null ? null : form.getType().name(),
                form.getTitle(),
                form.isRequired(),
                form.getMaxSelect(),
                form.isMultiple(),
                form.getMinDate(),
                form.getMaxDate(),
                form.getChoices().stream()
                        .map(choice -> new ChoiceView(choice.getContent(), choice.getImagePath()))
                        .toList());
    }
}
