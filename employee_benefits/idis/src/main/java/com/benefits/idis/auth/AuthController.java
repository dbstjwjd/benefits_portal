package com.benefits.idis.auth;

import com.benefits.idis.employee.Employee;
import com.benefits.idis.setting.SiteSettingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    /** 이름+전화번호까지 통과하고 PIN 을 기다리는 관리자. 아직 로그인된 것이 아니다. */
    static final String PENDING_ADMIN = "pendingAdminEmpNo";
    /** PIN 은 맞았지만 강제 변경이 남은 관리자. 변경을 마쳐야 세션이 생긴다. */
    static final String PIN_CHANGE_PENDING = "pinChangePendingEmpNo";

    private final AuthService authService;
    private final PinService pinService;
    private final SiteSettingService siteSettingService;

    @GetMapping("/login")
    public String loginForm(Model model) {
        addContactSettings(model);
        return "login";
    }

    /** 문의 모달 문구·담당자 목록. 설정이 비어 있어도 모달이 깨지지 않도록 기본값을 둔다. */
    private void addContactSettings(Model model) {
        model.addAttribute("loginNotice", siteSettingService.text(
                SiteSettingService.LOGIN_NOTICE, "복리후생 신청은 이곳에서 진행합니다"));

        var settings = siteSettingService.contactSettings();
        model.addAttribute("contactTitle",
                siteSettingService.textOr(settings, SiteSettingService.CONTACT_TITLE, "사이트 이용 문의"));
        model.addAttribute("contactIntro",
                siteSettingService.textOr(settings, SiteSettingService.CONTACT_INTRO, ""));
        model.addAttribute("contactFootnote",
                siteSettingService.textOr(settings, SiteSettingService.CONTACT_FOOTNOTE, ""));
        model.addAttribute("contactPersons", siteSettingService.contactPersons(settings));
    }

    /**
     * 1단계: 이름 + 전화번호.
     * 직원은 여기서 바로 로그인되고, 관리자는 PIN 화면으로 넘어간다.
     */
    @PostMapping("/login")
    public String login(@ModelAttribute LoginForm form, HttpSession session) {
        Optional<Employee> found = authService.authenticate(form.getName(), form.getPhone());
        if (found.isEmpty()) {
            return "redirect:/login?error";
        }
        Employee employee = found.get();

        if (!employee.isAdmin()) {
            session.setAttribute("loginUser", LoginUser.from(employee));
            return "redirect:/forms";
        }

        // 관리자인데 PIN 이 없으면 들어올 방법이 없다. 발급을 받아야 한다.
        if (!employee.hasPin()) {
            return "redirect:/login?nopin";
        }

        session.setAttribute(PENDING_ADMIN, employee.getEmpNo());
        return "redirect:/login/pin";
    }

    /* ── 2단계: PIN ──────────────────────────────────────── */

    @GetMapping("/login/pin")
    public String pinForm(HttpSession session, Model model) {
        String empNo = pending(session, PENDING_ADMIN);
        if (empNo == null) {
            return "redirect:/login";
        }
        model.addAttribute("pinLength", PinPolicy.LENGTH);
        return "login-pin";
    }

    @PostMapping("/login/pin")
    public String pin(@RequestParam(required = false) String pin,
                      HttpSession session, RedirectAttributes redirect) {
        String empNo = pending(session, PENDING_ADMIN);
        if (empNo == null) {
            return "redirect:/login";
        }

        PinService.Result result = pinService.verify(empNo, pin);
        if (!result.accepted()) {
            redirect.addFlashAttribute("error", result.message());
            return "redirect:/login/pin";
        }

        session.removeAttribute(PENDING_ADMIN);
        if (result.changeRequired()) {
            session.setAttribute(PIN_CHANGE_PENDING, empNo);
            return "redirect:/login/pin/change";
        }
        return finishLogin(session, empNo);
    }

    /* ── 3단계: 첫 로그인 PIN 변경 ───────────────────────── */

    @GetMapping("/login/pin/change")
    public String pinChangeForm(HttpSession session, Model model) {
        if (pending(session, PIN_CHANGE_PENDING) == null) {
            return "redirect:/login";
        }
        model.addAttribute("pinLength", PinPolicy.LENGTH);
        return "login-pin-change";
    }

    @PostMapping("/login/pin/change")
    public String pinChange(@RequestParam(required = false) String newPin,
                            @RequestParam(required = false) String confirmPin,
                            HttpSession session, RedirectAttributes redirect) {
        String empNo = pending(session, PIN_CHANGE_PENDING);
        if (empNo == null) {
            return "redirect:/login";
        }
        try {
            pinService.changeAfterForcedLogin(empNo, newPin, confirmPin);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/login/pin/change";
        }
        session.removeAttribute(PIN_CHANGE_PENDING);
        return finishLogin(session, empNo);
    }

    /** PIN 까지 끝난 뒤에야 세션을 만든다. */
    private String finishLogin(HttpSession session, String empNo) {
        Employee employee = authService.findActive(empNo).orElse(null);
        if (employee == null) {
            return "redirect:/login";
        }
        session.setAttribute("loginUser", LoginUser.from(employee));
        return "redirect:/admin";
    }

    private static String pending(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        return value instanceof String empNo ? empNo : null;
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
