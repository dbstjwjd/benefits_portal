package com.benefits.idis.employee;

import com.benefits.idis.auth.LoginUser;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 본인의 기본 배송지 관리. 신청내역 화면에서만 쓴다.
 * 관리자 화면에는 넣지 않는다 — 개인 배송지라 남이 볼 이유가 없다.
 */
@Controller
@RequestMapping("/responses/default-address")
@RequiredArgsConstructor
public class DefaultAddressController {

    private final EmployeeRepository employeeRepository;

    @PostMapping
    @Transactional
    public String save(@RequestParam(required = false) String zipcode,
                       @RequestParam(required = false) String address,
                       @RequestParam(required = false) String detail,
                       HttpSession session, RedirectAttributes redirect) {
        Employee employee = current(session);
        if (employee == null) {
            return "redirect:/login";
        }
        String zip = trimmed(zipcode);
        String road = trimmed(address);
        if (zip.isEmpty() || road.isEmpty()) {
            redirect.addFlashAttribute("addressError", "우편번호 검색으로 주소를 골라주세요");
            return "redirect:/responses";
        }
        employee.changeDefaultAddress(zip, road, trimmed(detail));
        redirect.addFlashAttribute("addressToast", "기본 배송지를 저장했습니다");
        return "redirect:/responses";
    }

    @PostMapping("/delete")
    @Transactional
    public String delete(HttpSession session, RedirectAttributes redirect) {
        Employee employee = current(session);
        if (employee == null) {
            return "redirect:/login";
        }
        employee.clearDefaultAddress();
        redirect.addFlashAttribute("addressToast", "기본 배송지를 삭제했습니다");
        return "redirect:/responses";
    }

    /** 권한·재직 여부가 바뀔 수 있어 세션이 아니라 DB 에서 다시 읽는다. */
    private Employee current(HttpSession session) {
        if (!(session.getAttribute("loginUser") instanceof LoginUser loginUser)) {
            return null;
        }
        return employeeRepository.findById(loginUser.empNo()).filter(Employee::isActive).orElse(null);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.strip();
    }
}
