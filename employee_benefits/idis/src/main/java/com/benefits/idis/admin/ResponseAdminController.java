package com.benefits.idis.admin;

import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Employee;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin/responses")
@RequiredArgsConstructor
public class ResponseAdminController {

    private final ResponseAdminService responseAdminService;
    private final ResponseExcelService responseExcelService;
    private final DepartmentRepository departmentRepository;

    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long formId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String tab,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("menu", "responses");

        List<FormOption> options = responseAdminService.options();
        model.addAttribute("options", options);

        Long target = formId != null ? formId : responseAdminService.defaultFormId();
        if (target == null) {
            // 폼이 하나도 없으면 고를 것도 없어 안내만 보여준다
            model.addAttribute("noForms", true);
            return "admin/responses";
        }

        ResponseSearch search = new ResponseSearch(keyword, departmentId, status, tab, page);
        ResponseListView view = responseAdminService.load(target, search);

        model.addAttribute("noForms", false);
        model.addAttribute("search", search);
        model.addAttribute("overview", view.overview());
        model.addAttribute("rows", view.rows());
        model.addAttribute("stats", view.stats());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("columns", ExcelColumn.values());
        model.addAttribute("defaultColumns", ExcelColumn.DEFAULTS);
        return "admin/responses";
    }

    /** 목록의 눈 아이콘이 fetch 로 부르므로 실패도 JSON 으로 돌려줘야 한다. */
    @GetMapping("/{formId:\\d+}/detail/{empNo}")
    @ResponseBody
    public ResponseDetail detail(@PathVariable Long formId, @PathVariable String empNo) {
        return responseAdminService.detail(formId, empNo);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> export(@RequestParam Long formId,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long departmentId,
                                         @RequestParam(required = false) List<String> columns,
                                         @RequestParam(defaultValue = "false") boolean includePending) {
        // 미응답자 포함 여부는 모달에서 고른 값이 화면 필터보다 우선한다
        String status = includePending ? ResponseSearch.ALL : ResponseSearch.ANSWERED;
        ResponseSearch search = new ResponseSearch(keyword, departmentId, status,
                ResponseSearch.TAB_LIST, 0);
        List<ResponseRow> rows = responseAdminService.rowsForExport(formId, search);
        byte[] body = responseExcelService.export(formId, rows, ExcelColumn.parse(columns));

        String filename = "응답_" + formId + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalid(IllegalArgumentException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return "redirect:/admin/responses";
    }
}
