package com.benefits.idis.admin;

/** 설정 화면의 부서 한 줄. 소속 인원이 남아 있으면 지울 수 없다. */
public record DepartmentRow(Long id, String name, long memberCount) {

    public boolean deletable() {
        return memberCount == 0;
    }
}
