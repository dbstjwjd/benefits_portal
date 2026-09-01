package com.benefits.idis.admin;

import com.benefits.idis.auth.LoginUser;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /admin/** 접근 통제.
 * 세션 타임아웃이 30일이라 그 사이 권한이 바뀔 수 있으므로 role 은 항상 DB 에서 다시 읽는다.
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final EmployeeRepository employeeRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("loginUser") instanceof LoginUser loginUser)) {
            response.sendRedirect("/login");
            return false;
        }

        Employee employee = employeeRepository.findById(loginUser.empNo()).orElse(null);
        if (employee == null || !employee.isActive()) {
            session.invalidate();
            response.sendRedirect("/login");
            return false;
        }
        if (!employee.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        request.setAttribute("adminEmployee", employee);
        return true;
    }
}
