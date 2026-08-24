package com.benefits.idis.response;

import com.benefits.idis.common.BaseEntity;
import com.benefits.idis.employee.Employee;
import com.benefits.idis.form.Form;
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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"form_id", "emp_no"}))
public class Response extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_no", nullable = false)
    private Employee employee;

    /**
     * 실제 내용 수정 시각. answers 는 mappedBy 관계라 갈아끼워도 이 행이 UPDATE 되지 않아
     * updatedAt 만으로는 수정 여부를 알 수 없다 (반대로 최초 제출에서도 미세하게 어긋난다).
     */
    private LocalDateTime editedAt;

    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    public void addAnswer(Answer answer) {
        answers.add(answer);
        answer.assignResponse(this);
    }

    public void clearAnswers() {
        answers.clear();
    }

    public void markEdited() {
        this.editedAt = LocalDateTime.now();
    }
}
