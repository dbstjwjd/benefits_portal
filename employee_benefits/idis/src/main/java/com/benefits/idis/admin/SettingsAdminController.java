package com.benefits.idis.admin;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.setting.ContactPerson;
import com.benefits.idis.setting.SiteSettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SettingsAdminController {

    private final SiteSettingService siteSettingService;
    private final DepartmentAdminService departmentAdminService;

    @ModelAttribute("admin")
    public Employee admin(@RequestAttribute("adminEmployee") Employee adminEmployee) {
        return adminEmployee;
    }

    @GetMapping
    public String settings(Model model) {
        Map<String, String> contact = siteSettingService.contactSettings();

        model.addAttribute("menu", "settings");
        model.addAttribute("loginNotice", siteSettingService.text(SiteSettingService.LOGIN_NOTICE, ""));
        model.addAttribute("contactTitle",
                siteSettingService.textOr(contact, SiteSettingService.CONTACT_TITLE, ""));
        model.addAttribute("contactIntro",
                siteSettingService.textOr(contact, SiteSettingService.CONTACT_INTRO, ""));
        model.addAttribute("contactFootnote",
                siteSettingService.textOr(contact, SiteSettingService.CONTACT_FOOTNOTE, ""));
        model.addAttribute("contactPersons", siteSettingService.contactPersons(contact));
        model.addAttribute("departments", departmentAdminService.rows());
        return "admin/settings";
    }

    @PostMapping("/login-notice")
    public String saveLoginNotice(@RequestParam(required = false) String notice,
                                  RedirectAttributes redirect) {
        siteSettingService.save(Map.of(SiteSettingService.LOGIN_NOTICE,
                notice == null ? "" : notice.strip()));
        redirect.addFlashAttribute("toast", "로그인 안내 문구를 저장했습니다");
        return "redirect:/admin/settings";
    }

    @PostMapping("/contact")
    public String saveContact(@ModelAttribute ContactForm form, RedirectAttributes redirect) {
        List<ContactPerson> people = form.people();
        siteSettingService.save(Map.of(
                SiteSettingService.CONTACT_TITLE, nullToEmpty(form.getTitle()).strip(),
                SiteSettingService.CONTACT_INTRO, nullToEmpty(form.getIntro()).strip(),
                SiteSettingService.CONTACT_FOOTNOTE, nullToEmpty(form.getFootnote()).strip(),
                SiteSettingService.CONTACT_JSON, siteSettingService.toJson(people)));
        redirect.addFlashAttribute("toast", "문의 담당자를 저장했습니다");
        return "redirect:/admin/settings";
    }

    // ── 부서: 저장 버튼 없이 행 단위로 바로 반영한다 ──────────

    @PostMapping("/departments")
    @ResponseBody
    public Map<String, Object> createDepartment(@RequestParam String name) {
        try {
            DepartmentRow row = departmentAdminService.create(name);
            return Map.of("id", row.id(), "name", row.name(), "memberCount", row.memberCount());
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/departments/{id:\\d+}")
    @ResponseBody
    public Map<String, Object> renameDepartment(@PathVariable Long id, @RequestParam String name) {
        try {
            DepartmentRow row = departmentAdminService.rename(id, name);
            return Map.of("id", row.id(), "name", row.name(), "memberCount", row.memberCount());
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/departments/{id:\\d+}/delete")
    @ResponseBody
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        try {
            departmentAdminService.delete(id);
            return Map.of("deleted", true);
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleInvalid(IllegalArgumentException e, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", e.getMessage());
        return "redirect:/admin/settings";
    }
}
