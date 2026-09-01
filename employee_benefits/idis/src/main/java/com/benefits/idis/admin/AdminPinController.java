package com.benefits.idis.admin;

import com.benefits.idis.auth.PinPolicy;
import com.benefits.idis.auth.PinService;
import com.benefits.idis.employee.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자 본인의 PIN 변경. 현재 PIN 을 확인한 뒤에 바꾼다. */
@Controller
@RequestMapping("/admin/pin")
@RequiredArgsConstructor
public class AdminPinController {

    private final PinService pinService;

    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("pinLength", PinPolicy.LENGTH);
        return "admin/pin";
    }

    @PostMapping
    public String change(@RequestParam(required = false) String currentPin,
                         @RequestParam(required = false) String newPin,
                         @RequestParam(required = false) String confirmPin,
                         @RequestAttribute("adminEmployee") Employee adminEmployee,
                         RedirectAttributes redirect) {
        try {
            pinService.change(adminEmployee.getEmpNo(), currentPin, newPin, confirmPin);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/pin";
        }
        redirect.addFlashAttribute("toast", "PIN 을 변경했습니다");
        return "redirect:/admin/pin";
    }
}
