package com.benefits.idis.admin;

import java.time.LocalDateTime;
import java.util.List;

/** 목록의 눈 아이콘으로 여는 개별 응답. 질문 순서대로 답을 한 줄씩 보여준다. */
public record ResponseDetail(
        String name,
        String departmentName,
        LocalDateTime submittedAt,
        boolean edited,
        List<Item> items
) {

    /** 답을 안 한 질문도 빠뜨리지 않고 '-' 로 보여준다. */
    public record Item(String question, String answer) {
    }
}
