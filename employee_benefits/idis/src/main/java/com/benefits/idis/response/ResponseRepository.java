package com.benefits.idis.response;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    Optional<Response> findByFormIdAndEmployeeEmpNo(Long formId, String empNo);
    boolean existsByFormId(Long formId);
    long countByFormId(Long formId);
    List<Response> findByFormId(Long formId);
    @Query("select r from Response r join fetch r.form where r.employee.empNo = :empNo order by r.createdAt desc")
    List<Response> findWithFormByEmpNo(@Param("empNo") String empNo);

    @Query("select r.form.id from Response r where r.employee.empNo = :empNo")
    List<Long> findFormIdsByEmpNo(@Param("empNo") String empNo);
}
