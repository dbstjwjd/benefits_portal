package com.benefits.idis.admin;

import org.springframework.data.domain.Page;

/** 요약 카드는 필터와 무관하게 전체 폼을 세므로 목록과 같은 조회 결과에서 함께 만든다. */
public record FormListView(Page<FormRow> rows, FormSummary summary) {
}
