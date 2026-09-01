package com.benefits.idis.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 화면 위쪽의 폼 정보 한 줄과 요약 카드 네 개.
 * 응답 + 미응답 = 대상 인원이 되도록 셋 다 같은 목록에서 센다.
 */
public record ResponseOverview(
        Long formId,
        String title,
        LocalDateTime endAt,
        Long daysLeft,
        boolean closed,
        String typeLabel,
        List<String> departmentNames,
        long answered,
        long targetCount
) {

    public long pending() {
        return targetCount - answered;
    }

    public int ratePercent() {
        if (targetCount <= 0) {
            return 0;
        }
        return (int) Math.round(answered * 100.0 / targetCount);
    }

    /** 대상 칸에 배지를 그리지 않아도 되는 경우. */
    public boolean everyone() {
        return typeLabel == null && departmentNames.isEmpty();
    }

    /** 남은 기간 카드. 마감했거나 마감일이 없으면 숫자를 쓰지 않는다. */
    public String remainText() {
        if (closed) {
            return "마감";
        }
        if (daysLeft == null) {
            return "상시";
        }
        return daysLeft + "일";
    }
}
