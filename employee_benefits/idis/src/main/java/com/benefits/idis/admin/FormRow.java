package com.benefits.idis.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 폼 목록 한 줄. 대상 표기와 응답률처럼 화면에서만 쓰는 계산을 엔티티에 넣지 않으려고 따로 둔다.
 * targetCount 는 지금 재직 중인 대상 인원이라 폼을 만들 때와 값이 달라질 수 있다.
 */
public record FormRow(
        Long id,
        String title,
        String typeLabel,
        List<String> departmentNames,
        LocalDateTime endAt,
        Long daysLeft,
        boolean open,
        boolean closed,
        long responseCount,
        long targetCount
) {

    /** 구분·부서 조건이 모두 없으면 대상 칸에 배지 대신 '전체' 한 줄만 쓴다. */
    public boolean everyone() {
        return typeLabel == null && departmentNames.isEmpty();
    }

    /** 마감일 옆 배지 문구. 마감일이 없으면 null 이라 배지를 그리지 않는다. */
    public String dayBadge() {
        if (closed) {
            return "마감";
        }
        if (!open) {
            return "작성 중";
        }
        return daysLeft == null ? null : "D-" + daysLeft;
    }

    /** 마감 임박일 때만 배지를 강조한다. */
    public boolean urgent() {
        return open && daysLeft != null && daysLeft <= FormSummary.DUE_SOON_DAYS;
    }

    public int ratePercent() {
        if (targetCount <= 0) {
            return 0;
        }
        return (int) Math.round(responseCount * 100.0 / targetCount);
    }
}
