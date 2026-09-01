package com.benefits.idis.form;

import com.benefits.idis.common.BaseEntity;
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
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(nullable = false)
    private int sortOrder;

    @Column(columnDefinition = "json")
    private String config;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<Choice> choices = new ArrayList<>();

    void assignForm(Form form) {
        this.form = form;
    }

    public void addChoice(Choice choice) {
        choices.add(choice);
        choice.assignQuestion(this);
    }

    /** 화면과 검증이 같이 쓰는 타입별 부가 설정. 값이 없거나 깨졌으면 빈 설정이다. */
    public QuestionConfig configOrEmpty() {
        return QuestionConfig.parse(config);
    }

}
