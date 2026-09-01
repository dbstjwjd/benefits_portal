package com.benefits.idis.admin;

import com.benefits.idis.form.Form;
import com.benefits.idis.form.FormRepository;
import com.benefits.idis.form.Question;
import com.benefits.idis.form.QuestionType;
import com.benefits.idis.response.Answer;
import com.benefits.idis.response.Response;
import com.benefits.idis.response.ResponseRepository;
import com.benefits.idis.response.ResponseService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 응답 내역 엑셀. 고정 칸 뒤에 질문이 순서대로 붙는다.
 * 주소는 배송업체에 넘길 때 우편번호를 따로 써야 해서 세 칸으로 나눈다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponseExcelService {

    /** 고른 직원 정보 뒤에 늘 붙는 칸. */
    private static final List<String> ALWAYS = List.of("응답 시각", "수정 여부");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;
    private final ResponseService responseService;

    public byte[] export(Long formId, List<ResponseRow> rows, List<ExcelColumn> columns) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(formId)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));

        List<Question> questions = form.getQuestions();

        Map<String, Response> byEmpNo = new HashMap<>();
        responseRepository.findWithEmployeeByFormId(formId)
                .forEach(response -> byEmpNo.put(response.getEmployee().getEmpNo(), response));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("응답");
            List<String> headers = headers(columns, questions);
            writeRow(sheet, 0, headers);

            int index = 1;
            for (ResponseRow row : rows) {
                writeRow(sheet, index++, cells(row, columns, questions, byEmpNo.get(row.empNo())));
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.setColumnWidth(i, 5000);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("엑셀 파일을 만들지 못했습니다");
        }
    }

    private static List<String> headers(List<ExcelColumn> columns, List<Question> questions) {
        List<String> headers = new ArrayList<>(columns.stream().map(ExcelColumn::header).toList());
        headers.addAll(ALWAYS);
        for (Question question : questions) {
            if (question.getType() == QuestionType.ADDRESS) {
                headers.add(question.getTitle() + " (우편번호)");
                headers.add(question.getTitle() + " (기본주소)");
                headers.add(question.getTitle() + " (상세주소)");
            } else {
                headers.add(question.getTitle());
            }
        }
        return headers;
    }

    private List<String> cells(ResponseRow row, List<ExcelColumn> columns,
                               List<Question> questions, Response response) {
        List<String> cells = new ArrayList<>(columns.stream().map(column -> column.read(row)).toList());
        cells.add(row.submittedAt() == null ? "" : STAMP.format(row.submittedAt()));
        cells.add(row.edited() ? "수정" : "");

        Map<Long, Answer> byQuestion = new HashMap<>();
        if (response != null) {
            response.getAnswers().forEach(a -> byQuestion.put(a.getQuestion().getId(), a));
        }

        for (Question question : questions) {
            Answer answer = byQuestion.get(question.getId());
            if (question.getType() == QuestionType.ADDRESS) {
                cells.addAll(responseService.addressParts(answer));
            } else {
                cells.add(responseService.describe(answer));
            }
        }
        return cells;
    }

    private static void writeRow(Sheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }
}
