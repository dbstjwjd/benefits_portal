package com.benefits.idis.form;

import com.benefits.idis.auth.LoginUser;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.response.ResponseRepository;
import com.benefits.idis.response.ResponseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FormController {

    private final FormRepository formRepository;
    private final ResponseRepository responseRepository;
    private final EmployeeRepository employeeRepository;
    private final ResponseService responseService;

    @GetMapping("/forms")
    public String list(HttpSession session, Model model) {
        Employee employee = currentEmployee(session);
        if (employee == null) {
            return "redirect:/login";
        }

        List<Form> forms = formRepository.findByStatusAndDeletedAtIsNullOrderByEndAtAsc(FormStatus.OPEN).stream()
                .filter(Form::isOpen)
                .filter(form -> form.getTarget().includes(employee.getType()))
                .toList();

        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        model.addAttribute("forms", forms);
        model.addAttribute("submittedFormIds", responseRepository.findFormIdsByEmpNo(employee.getEmpNo()));
        return "forms";
    }

    /**
     * 신청 내역. /forms 는 지금 신청 가능한 폼만 보여주므로 마감된 폼은 거기서 사라진다.
     * 여기는 Response 기준으로 조회해서, 마감되거나 대상에서 빠진 폼도 이력이 남아 있다.
     */
    @GetMapping("/responses")
    public String myResponses(HttpSession session, Model model) {
        Employee employee = currentEmployee(session);
        if (employee == null) {
            return "redirect:/login";
        }

        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        model.addAttribute("responses", responseService.findMyResponses(employee.getEmpNo()));
        return "responses";
    }

    @GetMapping("/forms/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Employee employee = currentEmployee(session);
        if (employee == null) {
            return "redirect:/login";
        }
        Form form = formRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        boolean editable = form != null && isEditable(form, employee);
        boolean submitted = form != null && responseService.hasSubmitted(id, employee.getEmpNo());
        if (form == null || !(editable || submitted)) {
            // 마감/대상 변경으로 못 여는 폼이라도, 이미 제출한 이력이 있으면 읽기 전용으로는 보여준다.
            return "redirect:/forms";
        }

        model.addAttribute("form", form);
        model.addAttribute("values", responseService.loadValues(id, employee.getEmpNo()));
        model.addAttribute("submitted", submitted);
        model.addAttribute("readOnly", !editable);
        return "form-detail";
    }

    @PostMapping("/forms/{id}")
    public String submit(@PathVariable Long id,
            @RequestParam MultiValueMap<String, String> params,
            HttpSession session,
            Model model) {
        Employee employee = currentEmployee(session);
        if (employee == null) {
            return "redirect:/login";
        }
        Form form = accessibleForm(id, employee);
        if (form == null) {
            return "redirect:/forms";
        }

        List<String> errors = responseService.validate(form, params);
        if (!errors.isEmpty()) {
            // 입력한 값을 그대로 다시 보여준다. 화면은 values map 하나만 읽으므로 형태가 같다.
            model.addAttribute("form", form);
            model.addAttribute("values", params.toSingleValueMap());
            model.addAttribute("submitted", responseService.hasSubmitted(id, employee.getEmpNo()));
            model.addAttribute("readOnly", false);
            model.addAttribute("submitError", errors.getFirst());
            return "form-detail";
        }

        responseService.submit(form, employee, params);
        return "redirect:/forms/" + id + "/complete";
    }

    @GetMapping("/forms/{id}/complete")
    public String complete(@PathVariable Long id, HttpSession session, Model model) {
        Employee employee = currentEmployee(session);
        if (employee == null) {
            return "redirect:/login";
        }
        Form form = formRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (form == null || !responseService.hasSubmitted(id, employee.getEmpNo())) {
            return "redirect:/forms";
        }

        model.addAttribute("form", form);
        return "form-complete";
    }

    /**
     * 세션이 30일까지 유지되므로 대상 구분과 재직 여부는 세션 값이 아니라 DB 기준으로 판단한다.
     * 로그인 검사를 인터셉터로 빼지 않고 각 핸들러에서 한 줄로 호출한다.
     */
    private Employee currentEmployee(HttpSession session) {
        if (!(session.getAttribute("loginUser") instanceof LoginUser loginUser)) {
            return null;
        }
        Employee employee = employeeRepository.findById(loginUser.empNo()).orElse(null);
        if (employee == null || !employee.isActive()) {
            session.invalidate();
            return null;
        }
        return employee;
    }

    private static boolean isEditable(Form form, Employee employee) {
        return form.isOpen() && form.getTarget().includes(employee.getType());
    }

    /** 마감된 폼이나 대상이 아닌 폼을 URL 로 직접 열어 제출하는 경우를 막는다. */
    private Form accessibleForm(Long id, Employee employee) {
        Form form = formRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (form == null || !isEditable(form, employee)) {
            return null;
        }
        return form;
    }
}
