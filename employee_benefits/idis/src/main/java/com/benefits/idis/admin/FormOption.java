package com.benefits.idis.admin;

/** 폼 선택 드롭다운 한 줄. 진행 중과 마감을 나눠 보여준다. */
public record FormOption(Long id, String title, int ratePercent, boolean open) {
}
