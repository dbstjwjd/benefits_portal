package com.benefits.idis.form;

public enum QuestionType {
    SHORT_TEXT, LONG_TEXT, NUMBER,
    SINGLE_CHOICE, MULTI_CHOICE, DROPDOWN, IMAGE_CHOICE,
    DATE, ADDRESS, SCALE, FILE, SECTION;

    public boolean hasChoices() {
        return this == SINGLE_CHOICE || this == MULTI_CHOICE
                || this == DROPDOWN || this == IMAGE_CHOICE;
    }

    public boolean isAnswerable() {
        return this != SECTION;
    }
}
