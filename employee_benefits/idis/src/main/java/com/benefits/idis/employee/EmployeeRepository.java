package com.benefits.idis.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findByNameAndActiveTrue(String name);
    boolean existsByPhone(String phone);
    long countByActiveTrue();
    long countByDepartmentId(Long departmentId);
    long countByRoleAndActiveTrue(Role role);

    /** 폼 대상 인원을 셀 때 부서까지 한 번에 읽는다. */
    @Query("select e from Employee e left join fetch e.department where e.active = true")
    List<Employee> findActiveWithDepartment();

    boolean existsByPhoneAndEmpNoNot(String phone, String empNo);

    long countByHireDateBetween(LocalDate from, LocalDate to);

    /** 대시보드 '이번 달 입사자'. */
    @Query("""
            select e from Employee e
            left join fetch e.department
            where e.active = true and e.hireDate between :from and :to
            order by e.hireDate desc, e.name asc
            """)
    List<Employee> findHiredBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
    long countByResignDateBetween(LocalDate from, LocalDate to);

    /**
     * 관리자 직원 목록. 이름 검색 / 부서 / 구분 / 재직 상태를 조합한다.
     * 조건이 없으면(null) 그 항목은 무시한다. status: ACTIVE / RESIGNED / ALL 을 boolean 으로 받는다.
     */
    @Query("""
            select e from Employee e
            left join fetch e.department d
            where (:name is null or e.name like concat('%', :name, '%'))
              and (:departmentId is null or d.id = :departmentId)
              and (:type is null or e.type = :type)
              and (:active is null or e.active = :active)
            """)
    Page<Employee> search(@Param("name") String name,
                          @Param("departmentId") Long departmentId,
                          @Param("type") EmployeeType type,
                          @Param("active") Boolean active,
                          Pageable pageable);

    /** 엑셀 다운로드용. 페이지 없이 같은 조건으로 전부 가져온다. */
    @Query("""
            select e from Employee e
            left join fetch e.department d
            where (:name is null or e.name like concat('%', :name, '%'))
              and (:departmentId is null or d.id = :departmentId)
              and (:type is null or e.type = :type)
              and (:active is null or e.active = :active)
            order by e.name asc
            """)
    List<Employee> searchAll(@Param("name") String name,
                             @Param("departmentId") Long departmentId,
                             @Param("type") EmployeeType type,
                             @Param("active") Boolean active);
}
