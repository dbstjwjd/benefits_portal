package com.benefits.idis.admin;

import java.util.List;

/**
 * 엑셀 업로드 검증 결과.
 * 오류가 하나라도 있으면 반영을 허용하지 않는다.
 */
public record ExcelImportResult(
        int createCount,
        int updateCount,
        List<String> newDepartments,
        List<RowError> errors
) {
    public boolean hasError() {
        return !errors.isEmpty();
    }

    /** rowNumber 는 엑셀에서 보이는 행 번호(헤더 포함 1-based). */
    public record RowError(int rowNumber, String empNo, String reason) {
    }
}
