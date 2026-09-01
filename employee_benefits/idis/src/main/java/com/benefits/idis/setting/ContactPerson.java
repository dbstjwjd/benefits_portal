package com.benefits.idis.setting;

/**
 * 사이트 이용 문의 담당자. contact_json 의 항목 하나에 대응한다.
 * 내선은 표시만 하므로 문자열 그대로 둔다.
 */
public record ContactPerson(
        String name,
        String role,
        String location,
        String extension
) {
}
