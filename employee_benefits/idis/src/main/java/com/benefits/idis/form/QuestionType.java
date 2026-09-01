package com.benefits.idis.form;

/** 편집 화면에서 만들 수 있는 타입만 둔다. 여기 없는 값은 응답 화면도 그리지 못한다. */
public enum QuestionType {
    SHORT_TEXT, LONG_TEXT, PHONE,
    SINGLE_CHOICE, MULTI_CHOICE, IMAGE_CHOICE,
    DATE, ADDRESS;

    /** 화면에 쓰는 이름. 편집 화면의 타입 선택과 통계·엑셀이 같은 말을 쓰게 한다. */
    public String label() {
        return switch (this) {
            case SHORT_TEXT -> "단답";
            case LONG_TEXT -> "장문";
            case PHONE -> "전화번호";
            case SINGLE_CHOICE -> "단일선택";
            case MULTI_CHOICE -> "다중선택";
            case IMAGE_CHOICE -> "이미지 선택";
            case DATE -> "날짜";
            case ADDRESS -> "주소";
        };
    }

    public boolean hasChoices() {
        return this == SINGLE_CHOICE || this == MULTI_CHOICE || this == IMAGE_CHOICE;
    }
}
