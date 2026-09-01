package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.employee.EmployeeType;
import com.benefits.idis.employee.Role;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 임직원 엑셀 업로드/다운로드.
 * 컬럼 순서 고정: 사번 / 이름 / 부서 / 구분 / 전화번호 / 입사일
 * 사번 기준 upsert 이고, 파일에 없는 직원은 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
public class EmployeeExcelService {

    public static final List<String> HEADERS =
            List.of("사번", "이름", "부서", "구분", "전화번호", "입사일");

    private static final Pattern PHONE = Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    /* ── 업로드 양식 서식. 여기 값들은 template() 에서만 쓴다 ── */

    private static final String TEMPLATE_SHEET = "직원 등록";
    private static final String TEMPLATE_FONT = "맑은 고딕";
    private static final byte[] HEADER_RGB = {0x00, (byte) 0x9C, (byte) 0xA6};

    /** HEADERS 와 같은 순서. 사번 / 이름 / 부서 / 구분 / 전화번호 / 입사일 */
    private static final int[] TEMPLATE_WIDTHS = {12, 10, 14, 10, 16, 14};

    private static final int COL_TYPE = 3;
    private static final int COL_HIRE_DATE = 5;

    /** 드롭다운과 날짜 서식을 미리 깔아 둘 데이터 행 수. 시트의 2~101 행. */
    private static final int TEMPLATE_ROWS = 100;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /** 검증만 한다. DB 는 건드리지 않는다. */
    public ExcelImportResult validate(MultipartFile file) {
        List<ParsedRow> rows = new ArrayList<>();
        List<ExcelImportResult.RowError> errors = new ArrayList<>();
        parse(file, rows, errors);
        return summarize(rows, errors);
    }

    /** 검증을 다시 돌린 뒤 오류가 없을 때만 반영한다. */
    @Transactional
    public ExcelImportResult apply(MultipartFile file) {
        List<ParsedRow> rows = new ArrayList<>();
        List<ExcelImportResult.RowError> errors = new ArrayList<>();
        parse(file, rows, errors);
        ExcelImportResult result = summarize(rows, errors);
        if (result.hasError()) {
            return result;
        }

        for (ParsedRow row : rows) {
            Department department = row.department == null ? null
                    : departmentRepository.findByName(row.department)
                            .orElseGet(() -> departmentRepository.save(
                                    Department.builder().name(row.department).build()));

            employeeRepository.findById(row.empNo).ifPresentOrElse(
                    existing -> existing.update(row.name, row.phone, row.type, department, row.hireDate),
                    () -> employeeRepository.save(Employee.builder()
                            .empNo(row.empNo)
                            .name(row.name)
                            .phone(row.phone)
                            .type(row.type)
                            .role(Role.EMPLOYEE)
                            .department(department)
                            .hireDate(row.hireDate)
                            .build()));
        }
        return result;
    }

    /**
     * 업로드 양식. 예시 행 없이 헤더만 두고 서식을 입힌다.
     * 값만 담는 export() 와 달리 서식이 필요해 write() 를 쓰지 않는다.
     */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 기본 글꼴을 바꿔 두면 아직 값이 없는 칸까지 같은 글꼴로 입력된다
            workbook.getFontAt(0).setFontName(TEMPLATE_FONT);

            Sheet sheet = workbook.createSheet(TEMPLATE_SHEET);

            Row head = sheet.createRow(0);
            head.setHeightInPoints(28);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < TEMPLATE_WIDTHS.length; i++) {
                sheet.setColumnWidth(i, TEMPLATE_WIDTHS[i] * 256);
            }
            // 비어 있는 칸에도 날짜 서식이 걸려 있어야 2026-03-02 로 입력된다
            sheet.setDefaultColumnStyle(COL_HIRE_DATE, dateStyle(workbook));

            sheet.createFreezePane(0, 1);
            addTypeDropdown(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 파일을 만들 수 없습니다", e);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName(TEMPLATE_FONT);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(HEADER_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle dateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private static void addTypeDropdown(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint rule = helper.createExplicitListConstraint(new String[]{"직접직", "간접직"});
        DataValidation validation = helper.createValidation(rule,
                new CellRangeAddressList(1, TEMPLATE_ROWS, COL_TYPE, COL_TYPE));
        // XSSF 는 이 값을 뒤집어 쓴다. true 여야 목록 화살표가 보인다.
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        validation.createErrorBox("구분", "직접직 또는 간접직만 넣을 수 있습니다.");
        sheet.addValidationData(validation);
    }

    public byte[] export(List<Employee> employees) {
        List<List<String>> rows = employees.stream()
                .map(e -> List.of(
                        e.getEmpNo(),
                        e.getName(),
                        e.getDepartment() == null ? "" : e.getDepartment().getName(),
                        e.getType() == EmployeeType.DIRECT ? "직접직" : "간접직",
                        e.getPhone(),
                        e.getHireDate() == null ? "" : e.getHireDate().toString()))
                .toList();
        return write(HEADERS, rows);
    }

    // ── 내부 ────────────────────────────────────────────────

    private void parse(MultipartFile file, List<ParsedRow> rows, List<ExcelImportResult.RowError> errors) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 선택해주세요");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("xlsx 파일만 업로드할 수 있습니다");
        }

        Set<String> seenEmpNo = new LinkedHashSet<>();
        Set<String> seenPhone = new LinkedHashSet<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int rowNumber = i + 1;
                String empNo = text(row.getCell(0));
                try {
                    rows.add(toParsedRow(row, empNo, seenEmpNo, seenPhone));
                } catch (IllegalArgumentException e) {
                    errors.add(new ExcelImportResult.RowError(rowNumber, empNo, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다");
        }
    }

    private ParsedRow toParsedRow(Row row, String empNo, Set<String> seenEmpNo, Set<String> seenPhone) {
        if (empNo.isBlank()) {
            throw new IllegalArgumentException("사번 누락");
        }
        if (!seenEmpNo.add(empNo)) {
            throw new IllegalArgumentException("파일 내 사번 중복");
        }

        String name = text(row.getCell(1));
        if (name.isBlank()) {
            throw new IllegalArgumentException("이름 누락");
        }

        String department = text(row.getCell(2));
        String typeText = text(row.getCell(3));
        EmployeeType type = switch (typeText) {
            case "직접직", "DIRECT" -> EmployeeType.DIRECT;
            case "간접직", "INDIRECT" -> EmployeeType.INDIRECT;
            default -> throw new IllegalArgumentException(
                    typeText.isBlank() ? "구분 누락" : "구분 값 오류 (직접직/간접직)");
        };

        String phone = text(row.getCell(4));
        if (phone.isBlank()) {
            throw new IllegalArgumentException("전화번호 누락");
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("전화번호 형식 오류");
        }
        if (!seenPhone.add(phone)) {
            throw new IllegalArgumentException("파일 내 전화번호 중복");
        }
        if (employeeRepository.existsByPhoneAndEmpNoNot(phone, empNo)) {
            throw new IllegalArgumentException("다른 직원이 쓰는 전화번호");
        }

        LocalDate hireDate = parseDate(row.getCell(5));
        return new ParsedRow(empNo, name, department.isBlank() ? null : department, type, phone, hireDate);
    }

    private ExcelImportResult summarize(List<ParsedRow> rows, List<ExcelImportResult.RowError> errors) {
        int create = 0;
        int update = 0;
        Set<String> newDepartments = new LinkedHashSet<>();
        for (ParsedRow row : rows) {
            if (employeeRepository.existsById(row.empNo)) {
                update++;
            } else {
                create++;
            }
            if (row.department != null && departmentRepository.findByName(row.department).isEmpty()) {
                newDepartments.add(row.department);
            }
        }
        return new ExcelImportResult(create, update, List.copyOf(newDepartments), List.copyOf(errors));
    }

    private static LocalDate parseDate(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = text(cell);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.replace('/', '-').replace('.', '-'));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("입사일 형식 오류 (yyyy-MM-dd)");
        }
    }

    private static boolean isBlankRow(Row row) {
        for (int c = 0; c < 6; c++) {
            if (!text(row.getCell(c)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().strip();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : stripTrailingZero(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /** 사번이 숫자로 들어오면 20240001.0 이 되므로 정수는 소수점을 떼어낸다. */
    private static String stripTrailingZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static byte[] write(List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("직원");
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                head.createCell(i).setCellValue(headers.get(i));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c));
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.setColumnWidth(i, 4000);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("엑셀 파일을 만들 수 없습니다", e);
        }
    }

    private record ParsedRow(String empNo, String name, String department, EmployeeType type,
                             String phone, LocalDate hireDate) {
    }
}
