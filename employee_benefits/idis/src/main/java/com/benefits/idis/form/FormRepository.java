package com.benefits.idis.form;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    Optional<Form> findByIdAndDeletedAtIsNull(Long id);
    List<Form> findByStatusAndDeletedAtIsNullOrderByEndAtAsc(FormStatus status);

    /**
     * 관리자 목록용. 진행 중·마감 판정이 시각에 따라 갈리고 대상 부서까지 함께 봐야 해서
     * 조건을 쿼리로 쪼개지 않고 삭제되지 않은 폼을 한 번에 읽어 서비스에서 거른다.
     */
    @Query("select distinct f from Form f left join fetch f.targetDepartments where f.deletedAt is null")
    List<Form> findAllWithTargets();

    /** 삭제됨 탭 전용. 지운 폼만 본다. */
    @Query("select distinct f from Form f left join fetch f.targetDepartments where f.deletedAt is not null")
    List<Form> findDeletedWithTargets();

    /** 부서를 지우기 전에 그 부서를 대상으로 하는 폼이 있는지 본다. */
    @Query("""
            select count(f) from Form f
            join f.targetDepartments d
            where d.id = :departmentId and f.deletedAt is null
            """)
    long countTargetingDepartment(@Param("departmentId") Long departmentId);
}
