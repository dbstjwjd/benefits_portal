package com.benefits.idis.admin;

import com.benefits.idis.form.FormTarget;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** 폼 편집 화면 입력. 질문은 화면에서 만든 순서대로 들어온다. */
@Getter
@Setter
@NoArgsConstructor
public class FormEditForm {

    private String title;
    private String description;

    private FormTarget target = FormTarget.ALL;

    /** 비우면 전체 부서가 대상이다. */
    private List<Long> departmentIds = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    private List<QuestionForm> questions = new ArrayList<>();

    /** 마감일. 날짜만 넣고 시각을 비우면 그날 끝까지로 본다. */
    public LocalDateTime endAt() {
        if (endDate == null) {
            return null;
        }
        return endDate.atTime(endTime != null ? endTime : LocalTime.of(23, 59));
    }
}
