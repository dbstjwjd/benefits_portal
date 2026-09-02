package com.benefits.idis.common;

import java.util.regex.Pattern;

/**
 * 휴대폰 번호 표기를 한 가지로 맞춘다.
 *
 * 저장 형태가 갈리면 전화번호에 걸어 둔 UNIQUE 와 중복 검사가 무력해진다.
 * ("010-1234-5678" 과 "01012345678" 은 서로 다른 문자열이라 둘 다 통과한다)
 * 들어오는 경로가 여럿이므로(직원 모달·엑셀 업로드) 판단을 여기 한 곳에 둔다.
 */
public final class PhoneFormat {

    private static final Pattern MOBILE = Pattern.compile("^01[016789][0-9]{7,8}$");

    private PhoneFormat() {
    }

    /**
     * 하이픈을 붙인 표준형으로 바꾼다. 휴대폰 번호가 아니면 null 이다.
     * 구분자는 무엇이 오든(없어도) 숫자만 뽑아서 다시 만든다.
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!MOBILE.matcher(digits).matches()) {
            return null;
        }
        int middle = digits.length() - 7;
        return digits.substring(0, 3) + "-"
                + digits.substring(3, 3 + middle) + "-"
                + digits.substring(3 + middle);
    }
}
