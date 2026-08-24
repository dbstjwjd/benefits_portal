package com.benefits.idis.response;

import java.time.LocalDateTime;

/**
 * "신청 내역" 화면 전용 조회 모델.
 * Form 이 마감되거나 대상에서 빠져도 신청 이력은 그대로 남아야 하므로
 * /forms 목록(진행중인 폼만 노출)과는 별도로 Response 기준으로 조회한다.
 */
public record MyResponseView(
        Long formId,
        String formTitle,
        boolean formOpen,
        LocalDateTime endAt,
        LocalDateTime submittedAt,
        boolean edited
) {
}
