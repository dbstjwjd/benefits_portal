package com.benefits.idis.form;

import com.benefits.idis.common.BaseEntity;
import com.benefits.idis.employee.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Form extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormTarget target;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    public void update(String title, String description, FormTarget target,
                       LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.description = description;
        this.target = target;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void changeEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public void open() {
        this.status = FormStatus.OPEN;
    }

    public void close() {
        this.status = FormStatus.CLOSED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.assignForm(this);
    }

    public boolean isOpen() {
        if (status != FormStatus.OPEN) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) return false;
        return endAt == null || now.isBefore(endAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
