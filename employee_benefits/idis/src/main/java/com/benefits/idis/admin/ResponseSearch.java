package com.benefits.idis.admin;

/**
 * 응답 현황의 목록 조건. status 는 응답(answered) / 미응답(pending) / 전체(all) 이고 기본은 응답이다.
 * tab 은 응답 목록(list) / 통계(stats).
 */
public record ResponseSearch(String keyword, Long departmentId, String status, String tab, int page) {

    public static final String ANSWERED = "answered";
    public static final String PENDING = "pending";
    public static final String ALL = "all";

    public static final String TAB_LIST = "list";
    public static final String TAB_STATS = "stats";

    public static final int PAGE_SIZE = 20;

    public ResponseSearch {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
        status = (status == null || status.isBlank()) ? ANSWERED : status;
        tab = TAB_STATS.equals(tab) ? TAB_STATS : TAB_LIST;
        page = Math.max(page, 0);
    }

    public boolean statsTab() {
        return TAB_STATS.equals(tab);
    }

    /** 목록이 비었을 때 문구. 응답이 아예 없는 경우와 조건에 안 걸리는 경우를 나눈다. */
    public String emptyMessage(boolean anyone) {
        if (!anyone) {
            return "아직 응답이 없습니다";
        }
        if (keyword != null || departmentId != null) {
            return "검색 결과가 없습니다";
        }
        return switch (status) {
            case ANSWERED -> "응답한 직원이 없습니다";
            case PENDING -> "미응답 직원이 없습니다";
            default -> "대상 직원이 없습니다";
        };
    }
}
