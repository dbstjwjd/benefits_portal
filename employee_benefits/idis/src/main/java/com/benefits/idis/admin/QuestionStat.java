package com.benefits.idis.admin;

import java.util.List;

/**
 * 통계 탭의 질문 한 개.
 * 선택형이면 선택지별 집계를 담고, 자유 입력형은 집계할 게 없어 목록에서 보라고 안내만 한다.
 */
public record QuestionStat(int number, String title, String typeLabel, boolean textual, List<Bar> bars) {

    /** 이미지 선택은 막대 대신 썸네일로 그린다. imagePath 가 있으면 이미지형이다. */
    public record Bar(String label, String imagePath, long count, int percent) {
    }

    public boolean imageType() {
        return !bars.isEmpty() && bars.getFirst().imagePath() != null;
    }
}
