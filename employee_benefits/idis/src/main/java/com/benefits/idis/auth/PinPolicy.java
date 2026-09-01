package com.benefits.idis.auth;

import java.time.Duration;
import java.util.regex.Pattern;

/** 관리자 PIN 규칙. 화면·서비스가 같은 값을 보게 한 곳에 모아 둔다. */
public final class PinPolicy {

    public static final int LENGTH = 6;
    public static final int MAX_FAIL = 5;
    public static final Duration LOCK_FOR = Duration.ofMinutes(10);

    private static final Pattern SIX_DIGITS = Pattern.compile("^[0-9]{6}$");

    private PinPolicy() {
    }

    public static boolean isValidFormat(String pin) {
        return pin != null && SIX_DIGITS.matcher(pin).matches();
    }

    /**
     * 형식 오류 메시지. 입력값 자체는 절대 메시지에 넣지 않는다.
     * PIN 은 로그·예외 어디에도 원문이 남으면 안 된다.
     */
    public static String formatMessage() {
        return "PIN 은 숫자 " + LENGTH + "자리여야 합니다";
    }
}
