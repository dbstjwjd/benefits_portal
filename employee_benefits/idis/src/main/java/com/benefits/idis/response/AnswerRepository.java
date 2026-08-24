package com.benefits.idis.response;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByResponseId(Long responseId);
    List<Answer> findByQuestionId(Long questionId);
}
