package com.benefits.idis.admin;

import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
public class EmployeeAdminController {

    private final EmployeeAdminService employeeAdminService;
    private final EmployeeExcelService employeeExcelService;
    private final DepartmentRepository departmentRepository;

    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String name,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) EmployeeType type,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir,
                       @RequestAttribute("adminEmployee") Employee adminEmployee,
                       Model model) {
        EmployeeSearch search = new EmployeeSearch(name, departmentId, type, status, page, sort, dir);
        Page<Employee> result = employeeAdminService.search(search);

        // 행마다 삭제 가능 여부를 미리 계산해 둔다. 화면과 서버가 같은 판단을 쓴다.
        Map<String, String> deleteBlocked = new HashMap<>();
        for (Employee employee : result.getContent()) {
            String reason = employeeAdminService.deleteBlockReason(employee, adminEmployee);
            if (reason != null) {
                deleteBlocked.put(employee.getEmpNo(), reason);
            }
        }

        // 목록 헤더. 순서가 곧 화면 순서이고, key 가 빈 칸은 정렬하지 않는다.
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("name", "이름");
        columns.put("dept", "부서");
        columns.put("type", "구분");
        columns.put("", "전화번호");
        columns.put("hireDate", "입사일");

        model.addAttribute("columns", columns);
        model.addAttribute("deleteBlocked", deleteBlocked);
        model.addAttribute("menu", "employees");
        model.addAttribute("search", search);
        model.addAttribute("summary", employeeAdminService.summary());
        model.addAttribute("employees", result);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("types", EmployeeType.values());
        return "admin/employees";
    }

    @PostMapping
    public String create(@ModelAttribute EmployeeForm form, RedirectAttributes redirect) {
        employeeAdminService.create(form);
        redirect.addFlashAttribute("toast", "직원을 추가했습니다");
        return "redirect:/admin/employees";
    }

    @PostMapping("/{empNo}")
    public String update(@PathVariable String empNo,
                         @ModelAttribute EmployeeForm form,
                         @RequestAttribute("adminEmployee") Employee adminEmployee,
                         RedirectAttributes redirect) {
        employeeAdminService.update(empNo, form, adminEmployee);
        redirect.addFlashAttribute("toast", "직원 정보를 수정했습니다");
        return "redirect:/admin/employees";
    }

    /** 물리 삭제. 잘못 등록한 직원을 지우는 용도로, 퇴사 처리와는 별개다. */
    @PostMapping("/{empNo}/delete")
    public String delete(@PathVariable String empNo,
                         @RequestAttribute("adminEmployee") Employee adminEmployee,
                         RedirectAttributes redirect) {
        employeeAdminService.delete(empNo, adminEmployee);
        redirect.addFlashAttribute("toast", "직원을 삭제했습니다");
        return "redirect:/admin/employees";
    }

    @PostMapping("/{empNo}/resign")
    public String resign(@PathVariable String empNo,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resignDate,
                         @RequestAttribute("adminEmployee") Employee adminEmployee,
                         RedirectAttributes redirect) {
        employeeAdminService.resign(empNo, resignDate, adminEmployee.getEmpNo());
        redirect.addFlashAttribute("toast", "퇴사 처리했습니다");
        return "redirect:/admin/employees";
    }

    // ── 엑셀 ────────────────────────────────────────────────

    /** 화면이 fetch 로 부르므로 실패도 JSON 으로 돌려줘야 한다. 리다이렉트 핸들러를 타면 안 된다. */
    @PostMapping("/excel/validate")
    @ResponseBody
    public ExcelImportResult validateExcel(@RequestParam("file") MultipartFile file) {
        try {
            return employeeExcelService.validate(file);
        } catch (IllegalArgumentException e) {
            return fileError(e);
        }
    }

    @PostMapping("/excel/apply")
    @ResponseBody
    public ExcelImportResult applyExcel(@RequestParam("file") MultipartFile file) {
        try {
            return employeeExcelService.apply(file);
        } catch (IllegalArgumentException e) {
            return fileError(e);
        }
    }

    private static ExcelImportResult fileError(IllegalArgumentException e) {
        return new ExcelImportResult(0, 0, List.of(),
                List.of(new ExcelImportResult.RowError(0, "", e.getMessage())));
    }

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> template() {
        return xlsx("직원_업로드_양식.xlsx", employeeExcelService.template());
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String name,
                                         @RequestParam(required = false) Long departmentId,
                                         @RequestParam(required = false) EmployeeType type,
                                         @RequestParam(required = false) String status) {
        // 엑셀은 화면 정렬과 무관하게 늘 이름 순으로 뽑는다
        EmployeeSearch search = new EmployeeSearch(name, departmentId, type, status, 0, null, null);
        byte[] body = employeeExcelService.export(employeeAdminService.searchAll(search));
        return xlsx("직원_목록.xlsx", body);
    }

    private ResponseEntity<byte[]> xlsx(String filename, byte[] body) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /**
     * 모달 저장은 폼 전송이라 실패 시 목록으로 돌아가 메시지를 보여준다.
     * @ExceptionHandler 에서는 RedirectAttributes 의 플래시가 저장되지 않아 FlashMap 을 직접 쓴다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalid(IllegalArgumentException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return "redirect:/admin/employees";
    }
}
