package com.benefits.idis.admin;

import org.springframework.data.domain.Page;

import java.util.List;

/** 응답 현황 한 화면. 통계 탭이 아니면 stats 는 비어 있다. */
public record ResponseListView(ResponseOverview overview, Page<ResponseRow> rows, List<QuestionStat> stats) {
}
