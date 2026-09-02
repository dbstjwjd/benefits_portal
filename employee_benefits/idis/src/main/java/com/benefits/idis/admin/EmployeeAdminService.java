package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.auth.PinService;
import com.benefits.idis.common.PhoneFormat;
import com.benefits.idis.employee.Role;
import com.benefits.idis.response.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ResponseRepository responseRepository;
    private final PinService pinService;

    public Page<Employee> search(EmployeeSearch search) {
        return employeeRepository.search(
                search.name(), search.departmentId(), search.type(), search.activeFlag(),
                PageRequest.of(search.page(), EmployeeSearch.PAGE_SIZE, search.sortOrder()));
    }

    public List<Employee> searchAll(EmployeeSearch search) {
        return employeeRepository.searchAll(
                search.name(), search.departmentId(), search.type(), search.activeFlag());
    }

    public EmployeeSummary summary() {
        YearMonth thisMonth = YearMonth.now();
        LocalDate from = thisMonth.atDay(1);
        LocalDate to = thisMonth.atEndOfMonth();
        return new EmployeeSummary(
                employeeRepository.countByActiveTrue(),
                departmentRepository.count(),
                employeeRepository.countByHireDateBetween(from, to),
                employeeRepository.countByResignDateBetween(from, to));
    }

    public record EmployeeSummary(long activeCount, long departmentCount, long hiredThisMonth, long resignedThisMonth) {
    }

    @Transactional
    public void create(EmployeeForm form) {
        String empNo = required(form.getEmpNo(), "사번을 입력해주세요");
        if (employeeRepository.existsById(empNo)) {
            throw new IllegalArgumentException("이미 등록된 사번입니다");
        }
        String phone = validateShared(form, null);

        employeeRepository.save(Employee.builder()
                .empNo(empNo)
                .name(form.getName().strip())
                .phone(phone)
                .type(form.getType())
                .role(Role.EMPLOYEE)
                .department(departmentOf(form.getDepartmentId()))
                .hireDate(form.getHireDate())
                .build());
    }

    @Transactional
    public void update(String empNo, EmployeeForm form, Employee actor) {
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다"));
        String phone = validateShared(form, empNo);

        employee.update(form.getName().strip(), phone, form.getType(),
                departmentOf(form.getDepartmentId()), form.getHireDate());

        // 역할 칸은 슈퍼 관리자에게만 그려지지만, 폼을 직접 만들어 보낼 수 있어 서버에서 다시 본다.
        Role role = form.getRole() == null ? employee.getRole() : form.getRole();
        boolean becameAdmin = role == Role.ADMIN && employee.getRole() != Role.ADMIN;
        if (role != employee.getRole()) {
            changeRole(employee, role, actor);
        }
        applyPin(employee, form.getInitialPin(), becameAdmin, actor);
        // superAdmin 은 입력으로 받지 않으므로 이 경로로는 켜지지도 꺼지지도 않는다.
    }

    /**
     * PIN 발급·초기화. 슈퍼 관리자만 할 수 있다.
     * 관리자로 올릴 때는 필수, 이미 관리자인 사람은 값이 있을 때만 초기화한다.
     * 관리자가 아니게 되면 PIN 관련 값을 전부 지운다.
     */
    private void applyPin(Employee employee, String initialPin, boolean becameAdmin, Employee actor) {
        if (!employee.isAdmin()) {
            employee.clearPin();
            return;
        }
        boolean given = initialPin != null && !initialPin.isBlank();
        if (becameAdmin && !given) {
            throw new IllegalArgumentException("관리자로 지정하려면 초기 PIN 을 입력해주세요");
        }
        if (!given) {
            return;
        }
        if (!actor.isSuperAdmin()) {
            throw new IllegalArgumentException("PIN 은 슈퍼 관리자만 발급할 수 있습니다");
        }
        // 발급받은 PIN 은 본인이 바꾸기 전까지 관리자 화면에 들어갈 수 없다
        employee.assignPin(pinService.hash(initialPin.strip()), true);
    }

    private void changeRole(Employee employee, Role role, Employee actor) {
        if (!actor.isSuperAdmin()) {
            throw new IllegalArgumentException("역할은 슈퍼 관리자만 변경할 수 있습니다");
        }
        // 슈퍼 관리자 계정은 본인 것이든 아니든 역할을 건드릴 수 없다.
        if (employee.isSuperAdmin()) {
            throw new IllegalArgumentException("슈퍼 관리자의 역할은 변경할 수 없습니다");
        }
        // 자기 자신의 관리자 권한을 스스로 내려 잠기는 것을 막는다.
        if (employee.getEmpNo().equals(actor.getEmpNo()) && role != Role.ADMIN) {
            throw new IllegalArgumentException("본인의 관리자 권한은 해제할 수 없습니다");
        }
        if (role != Role.ADMIN) {
            guardLastAdmin(employee);
        }
        employee.changeRole(role);
    }

    @Transactional
    public void resign(String empNo, LocalDate resignDate, String actingEmpNo) {
        if (empNo.equals(actingEmpNo)) {
            throw new IllegalArgumentException("본인은 퇴사 처리할 수 없습니다");
        }
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다"));
        if (employee.isSuperAdmin()) {
            throw new IllegalArgumentException("슈퍼 관리자는 퇴사 처리할 수 없습니다");
        }
        guardLastAdmin(employee);
        employee.resign(resignDate == null ? LocalDate.now() : resignDate);
    }

    /**
     * 잘못 등록한 직원을 지운다. 퇴사 처리(resign)와는 다른 기능이다.
     * 응답 이력이 남아 있으면 지우지 않는다 — 과거 기록이 고아가 되기 때문이다.
     */
    @Transactional
    public void delete(String empNo, Employee actor) {
        Employee employee = employeeRepository.findById(empNo)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다"));
        String reason = deleteBlockReason(employee, actor);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        employeeRepository.delete(employee);
    }

    /**
     * 삭제할 수 없는 이유. 지울 수 있으면 null 이다.
     * 화면(버튼 비활성 사유)과 서버(차단)가 같은 판단을 쓰도록 한 곳에 둔다.
     */
    public String deleteBlockReason(Employee employee, Employee actor) {
        if (employee.getEmpNo().equals(actor.getEmpNo())) {
            return "본인은 삭제할 수 없습니다";
        }
        if (employee.isSuperAdmin()) {
            return "슈퍼 관리자는 삭제할 수 없습니다";
        }
        if (employee.isAdmin() && !actor.isSuperAdmin()) {
            return "관리자는 슈퍼 관리자만 삭제할 수 있습니다";
        }
        if (responseRepository.existsByEmployeeEmpNo(employee.getEmpNo())) {
            return "응답 이력이 있어 삭제할 수 없습니다. 퇴사 처리를 사용해 주세요";
        }
        return null;
    }

    /**
     * 관리자가 하나도 남지 않으면 아무도 관리자 화면에 들어갈 수 없다.
     * 본인 관련 차단이 대부분을 먼저 걸러내지만, 다른 경로로 마지막 관리자를 내리는 것도 막아 둔다.
     */
    private void guardLastAdmin(Employee target) {
        // 이미 퇴사한 관리자는 인원에 안 들어가므로 정리하는 것을 막지 않는다
        if (target.getRole() == Role.ADMIN && target.isActive()
                && employeeRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("관리자는 최소 1명 이상 필요합니다");
        }
    }

    /** 공통 검증. 저장에 쓸 표준형 전화번호를 돌려준다. */
    private String validateShared(EmployeeForm form, String empNoOrNull) {
        required(form.getName(), "이름을 입력해주세요");
        String input = required(form.getPhone(), "전화번호를 입력해주세요");
        if (form.getType() == null) {
            throw new IllegalArgumentException("구분을 선택해주세요");
        }
        // 표기를 맞춰 두지 않으면 010-1234-5678 과 01012345678 이 둘 다 통과해
        // 같은 사람이 두 번 등록된다. UNIQUE 도 문자열이 달라 못 막는다.
        String phone = PhoneFormat.normalize(input);
        if (phone == null) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다");
        }
        boolean duplicated = empNoOrNull == null
                ? employeeRepository.existsByPhone(phone)
                : employeeRepository.existsByPhoneAndEmpNoNot(phone, empNoOrNull);
        if (duplicated) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다");
        }
        return phone;
    }

    private Department departmentOf(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다"));
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
