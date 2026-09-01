package com.benefits.idis.admin;

import com.benefits.idis.employee.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;

    /** 사이드바가 모든 화면에서 관리자 이름을 쓰므로 공통으로 넣어준다. */
    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("menu", "dashboard");
        model.addAttribute("view", dashboardService.load());
        return "admin/dashboard";
    }
}
