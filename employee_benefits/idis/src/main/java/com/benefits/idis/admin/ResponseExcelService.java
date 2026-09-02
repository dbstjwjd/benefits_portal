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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 응답 내역 엑셀.
 * 컬럼 순서는 [고른 직원 정보] → [응답 시각·수정 여부] → [고른 질문] 이고,
 * 각 묶음 안의 순서는 화면에서 정한 그대로 따른다.
 * 주소는 한 칸에 "(우편번호) 기본주소 상세주소" 로 넣는다.
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

    public byte[] export(Long formId, List<ResponseRow> rows,
                         List<ExcelColumn> columns, List<Long> questionIds) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(formId)
                .orElseThrow(() -> new IllegalArgumentException("폼을 찾을 수 없습니다"));

        List<Question> questions = pickQuestions(form, questionIds);

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

    /**
     * 고른 질문만, 고른 순서대로.
     * 파라미터가 없으면(주소창으로 직접 부른 경우) 폼의 원래 순서를 그대로 쓴다.
     */
    private static List<Question> pickQuestions(Form form, List<Long> questionIds) {
        if (questionIds == null) {
            return form.getQuestions();
        }
        Map<Long, Question> byId = form.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, question -> question));
        return questionIds.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static List<String> headers(List<ExcelColumn> columns, List<Question> questions) {
        List<String> headers = new ArrayList<>(columns.stream().map(ExcelColumn::header).toList());
        headers.addAll(ALWAYS);
        // 주소도 한 칸이라 질문 제목을 그대로 쓴다
        questions.forEach(question -> headers.add(question.getTitle()));
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
            cells.add(question.getType() == QuestionType.ADDRESS
                    ? responseService.addressOneLine(answer)
                    : responseService.describe(answer));
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
