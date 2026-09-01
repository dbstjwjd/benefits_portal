package com.benefits.idis.form;

import com.benefits.idis.common.BaseEntity;
import com.benefits.idis.employee.Department;
import com.benefits.idis.employee.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** 대상 부서. 비어 있으면 전체 부서다. 구분(target) 조건과 AND 로 걸린다. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "form_target_department",
            joinColumns = @JoinColumn(name = "form_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    @Builder.Default
    private Set<Department> targetDepartments = new LinkedHashSet<>();

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

    public void addQuestion(Question question) {
        questions.add(question);
        question.assignForm(this);
    }

    /** 대상 부서를 통째로 갈아끼운다. 빈 목록이면 전체 부서가 대상이다. */
    public void replaceTargetDepartments(Collection<Department> departments) {
        targetDepartments.clear();
        targetDepartments.addAll(departments);
    }

    /**
     * 질문을 통째로 갈아끼운다. 순서 변경·복제·삭제를 한 번에 반영하려고 남은 것을 지우고 다시 붙인다.
     * 응답이 있는 폼은 질문 구조를 못 바꾸게 막혀 있어 이 경로로 들어오지 않는다.
     */
    public void replaceQuestions(List<Question> replacements) {
        questions.clear();
        for (Question question : replacements) {
            addQuestion(question);
        }
    }

    public boolean isOpen() {
        if (status != FormStatus.OPEN) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startAt != null && now.isBefore(startAt)) return false;
        return endAt == null || now.isBefore(endAt);
    }

    /** 마감 여부. 상태가 CLOSED 이거나 마감일이 지난 경우다. */
    public boolean isClosed() {
        if (status == FormStatus.CLOSED) {
            return true;
        }
        return endAt != null && LocalDateTime.now().isAfter(endAt);
    }

    /**
     * 이 직원이 폼 대상인지. 구분과 부서를 AND 로 본다.
     * 홈 폼 목록과 관리자 화면의 대상 인원 계산이 이 판정 하나를 같이 쓴다.
     */
    public boolean includes(Employee employee) {
        if (!target.includes(employee.getType())) {
            return false;
        }
        if (targetDepartments.isEmpty()) {
            return true;
        }
        Department department = employee.getDepartment();
        return department != null
                && targetDepartments.stream().anyMatch(d -> d.getId().equals(department.getId()));
    }

    /**
     * 마감까지 남은 일수. 마감일이 없으면 null, 이미 지났으면 음수.
     * 홈 목록의 D-day 배지에 쓴다.
     */
    public Long daysUntilEnd() {
        if (endAt == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), endAt);
    }
}
