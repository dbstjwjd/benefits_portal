package com.benefits.idis.form;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    Optional<Form> findByIdAndDeletedAtIsNull(Long id);
    List<Form> findByDeletedAtIsNullOrderByCreatedAtDesc();
    List<Form> findByStatusAndDeletedAtIsNullOrderByEndAtAsc(FormStatus status);
    long countByStatusAndDeletedAtIsNull(FormStatus status);
}
