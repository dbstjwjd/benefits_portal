package com.benefits.idis.admin;

/** 폼 관리 상단 요약 카드. 검색·필터와 무관하게 전체 폼 기준으로 센다. */
public record FormSummary(long totalCount, long openCount, long dueSoonCount, long endingThisMonth) {

    /** 마감 임박으로 볼 남은 일수. */
    public static final long DUE_SOON_DAYS = 3;
}
