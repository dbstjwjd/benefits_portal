package com.benefits.idis.admin;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 응답 엑셀에서 고를 수 있는 직원 정보 칸.
 * 응답 시각·수정 여부와 질문별 답변은 늘 들어가고, 여기 있는 것만 화면에서 켜고 끈다.
 */
public enum ExcelColumn {

    EMP_NO("empNo", "사번", ResponseRow::empNo),
    NAME("name", "이름", ResponseRow::name),
    DEPARTMENT("department", "부서", row -> row.departmentName() == null ? "" : row.departmentName()),
    TYPE("type", "구분", ResponseRow::typeLabel),
    PHONE("phone", "전화번호", row -> row.phone() == null ? "" : row.phone()),
    HIRE_DATE("hireDate", "입사일", row -> row.hireDate() == null
            ? "" : DateTimeFormatter.ISO_DATE.format(row.hireDate()));

    /** 아무것도 안 고르고 내려받는 것을 막기 위한 기본값. 시안의 기본 체크와 같다. */
    public static final List<ExcelColumn> DEFAULTS = List.of(EMP_NO, NAME, DEPARTMENT);

    private final String key;
    private final String header;
    private final Function<ResponseRow, String> reader;

    ExcelColumn(String key, String header, Function<ResponseRow, String> reader) {
        this.key = key;
        this.header = header;
        this.reader = reader;
    }

    public String key() {
        return key;
    }

    public String header() {
        return header;
    }

    public String read(ResponseRow row) {
        return reader.apply(row);
    }

    /** 화면에서 넘어온 키 목록을 순서대로 정리한다. 비어 있으면 기본값을 쓴다. */
    public static List<ExcelColumn> parse(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return DEFAULTS;
        }
        List<ExcelColumn> picked = Arrays.stream(values())
                .filter(column -> keys.contains(column.key))
                .toList();
        return picked.isEmpty() ? DEFAULTS : picked;
    }
}
