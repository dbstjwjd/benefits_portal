package com.benefits.idis.response;

import com.benefits.idis.form.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private Response response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String value;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerChoice> answerChoices = new ArrayList<>();

    void assignResponse(Response response) {
        this.response = response;
    }

    public void addChoice(AnswerChoice answerChoice) {
        answerChoices.add(answerChoice);
        answerChoice.assignAnswer(this);
    }

    public boolean isEmpty() {
        boolean noValue = value == null || value.isBlank();
        return noValue && answerChoices.isEmpty();
    }
}
