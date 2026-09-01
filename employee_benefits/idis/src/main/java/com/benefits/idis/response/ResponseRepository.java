package com.benefits.idis.response;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    Optional<Response> findByFormIdAndEmployeeEmpNo(Long formId, String empNo);

    /** 직원 완전 삭제 전에 응답 이력이 있는지 본다. */
    boolean existsByEmployeeEmpNo(String empNo);
    boolean existsByFormId(Long formId);
    long countByFormId(Long formId);
    @Query("select r from Response r join fetch r.form where r.employee.empNo = :empNo order by r.createdAt desc")
    List<Response> findWithFormByEmpNo(@Param("empNo") String empNo);

    /** 응답 현황 목록. 이름·부서를 같이 쓰므로 한 번에 읽는다. */
    @Query("""
            select r from Response r
            join fetch r.employee e
            left join fetch e.department
            where r.form.id = :formId
            """)
    List<Response> findWithEmployeeByFormId(@Param("formId") Long formId);

    /** 통계용 선택지 집계. 응답을 통째로 읽으면 조인이 커져서 개수만 따로 센다. */
    @Query("""
            select ac.choice.id, count(ac)
            from AnswerChoice ac
            where ac.answer.response.form.id = :formId
            group by ac.choice.id
            """)
    List<Object[]> countChoicesByFormId(@Param("formId") Long formId);

    /** 대시보드 '오늘 응답'. */
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /** 대시보드 '최근 응답'. 이름과 폼 제목을 같이 쓰므로 한 번에 읽는다. */
    @Query("""
            select r from Response r
            join fetch r.employee
            join fetch r.form
            order by r.createdAt desc
            """)
    List<Response> findRecent(Pageable pageable);

    /** 폼 선택 드롭다운의 응답률. 폼마다 세지 않고 한 번에 읽는다. */
    @Query("select r.form.id, count(r) from Response r group by r.form.id")
    List<Object[]> countGroupedByFormId();

}
