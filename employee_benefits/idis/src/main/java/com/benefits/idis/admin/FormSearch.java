package com.benefits.idis.admin;

/**
 * 폼 목록 검색 조건. status 는 진행 중(open) / 마감(closed) / 전체(all) 세 가지이고 기본은 진행 중이다.
 * 작성 중(DRAFT)이나 아직 시작 전인 폼은 진행 중에도 마감에도 걸리지 않아 전체에서만 보인다.
 */
public record FormSearch(String keyword, String status, int page) {

    public static final String OPEN = "open";
    public static final String CLOSED = "closed";
    public static final String ALL = "all";
    /** 지운 폼만 본다. 다른 상태에는 섞이지 않는다. */
    public static final String DELETED = "deleted";

    public static final int PAGE_SIZE = 20;

    public FormSearch {
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();
        status = (status == null || status.isBlank()) ? OPEN : status;
        page = Math.max(page, 0);
    }

    /**
     * 목록이 비었을 때 문구. 폼 자체가 없는 첫 진입과,
     * 폼은 있는데 조건에 걸리는 게 없는 경우를 구분한다.
     */
    /** 삭제됨 탭인지. 목록을 다른 곳에서 읽어야 해서 따로 본다. */
    public boolean deletedTab() {
        return DELETED.equals(status);
    }

    public String emptyMessage(boolean anyForm) {
        if (deletedTab()) {
            return "삭제한 폼이 없습니다";
        }
        if (!anyForm) {
            return "등록된 폼이 없습니다";
        }
        if (keyword != null) {
            return "검색 결과가 없습니다";
        }
        return switch (status) {
            case CLOSED -> "마감된 폼이 없습니다";
            case DELETED -> "삭제한 폼이 없습니다";
            case ALL -> "등록된 폼이 없습니다";
            default -> "진행 중인 폼이 없습니다";
        };
    }
}
