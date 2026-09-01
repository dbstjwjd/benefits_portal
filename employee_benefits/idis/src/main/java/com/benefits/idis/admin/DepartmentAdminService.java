package com.benefits.idis.admin;

import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.DepartmentRepository;
import com.benefits.idis.employee.EmployeeRepository;
import com.benefits.idis.form.FormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 부서 관리. 저장 버튼 없이 행 단위로 바로 반영한다.
 * 이름은 유일해야 하고, 소속 인원이나 대상으로 잡힌 폼이 있으면 지울 수 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentAdminService {

    private static final int MAX_NAME = 50;

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final FormRepository formRepository;

    public List<DepartmentRow> rows() {
        return departmentRepository.findAll().stream()
                .map(department -> new DepartmentRow(department.getId(), department.getName(),
                        employeeRepository.countByDepartmentId(department.getId())))
                .sorted(Comparator.comparing(DepartmentRow::name))
                .toList();
    }

    @Transactional
    public DepartmentRow create(String name) {
        String clean = validateName(name, null);
        Department saved = departmentRepository.save(Department.builder().name(clean).build());
        return new DepartmentRow(saved.getId(), saved.getName(), 0);
    }

    @Transactional
    public DepartmentRow rename(Long id, String name) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다"));
        department.rename(validateName(name, id));
        return new DepartmentRow(id, department.getName(), employeeRepository.countByDepartmentId(id));
    }

    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다"));

        if (employeeRepository.countByDepartmentId(id) > 0) {
            throw new IllegalArgumentException("소속 인원이 있어 삭제할 수 없습니다");
        }
        // 폼 대상에 걸려 있으면 지울 수 없다. 지우면 그 폼의 대상 조건이 조용히 바뀐다.
        if (formRepository.countTargetingDepartment(id) > 0) {
            throw new IllegalArgumentException("이 부서를 대상으로 하는 폼이 있어 삭제할 수 없습니다");
        }
        departmentRepository.delete(department);
    }

    private String validateName(String name, Long selfId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("부서명을 입력해주세요");
        }
        String clean = name.strip();
        if (clean.length() > MAX_NAME) {
            throw new IllegalArgumentException("부서명은 " + MAX_NAME + "자까지 넣을 수 있습니다");
        }
        departmentRepository.findByName(clean)
                .filter(found -> !found.getId().equals(selfId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("이미 있는 부서명입니다");
                });
        return clean;
    }
}
