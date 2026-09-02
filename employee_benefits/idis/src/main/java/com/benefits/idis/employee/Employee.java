package com.benefits.idis.employee;

import com.benefits.idis.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Employee extends BaseEntity {

    @Id
    @Column(length = 20)
    private String empNo;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.EMPLOYEE;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * 슈퍼 관리자. 역할을 바꿀 수 있는 유일한 사람이고, 남이 건드릴 수 없다.
     * 화면에는 드러내지 않는다. 값은 시드로만 넣고 애플리케이션에서는 바꾸지 않는다.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean superAdmin = false;

    private LocalDate hireDate;

    private LocalDate resignDate;

    /*
     * 관리자 PIN. 이름+전화번호만으로 전 직원 개인정보에 닿지 않도록 한 단계 더 둔다.
     * 값은 BCrypt 해시만 저장하고, 원문은 어디에도 남기지 않는다(로그 포함).
     * EMPLOYEE 는 쓰지 않으므로 전부 null/0 이다.
     */
    @Column(length = 72)
    private String pinHash;

    /** 발급·초기화 직후 true. 본인이 바꾸기 전에는 관리자 화면에 들어갈 수 없다. */
    @Column(nullable = false)
    @Builder.Default
    private boolean pinChangeRequired = false;

    @Column(nullable = false)
    @Builder.Default
    private int pinFailCount = 0;

    /** 이 시각까지 잠금. null 이거나 지난 시각이면 풀린 상태다. */
    private LocalDateTime pinLockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /*
     * 기본 배송지. 주소 질문을 매번 다시 적지 않게 본인이 저장해 두는 값이다.
     * 관리자 화면·엑셀에는 넣지 않는다. 개인 배송지라 볼 이유가 없다.
     */
    @Column(length = 10)
    private String defaultZipcode;

    @Column(length = 200)
    private String defaultAddress;

    @Column(length = 200)
    private String defaultAddressDetail;

    public void update(String name, String phone, EmployeeType type, Department department, LocalDate hireDate) {
        this.name = name;
        this.phone = phone;
        this.type = type;
        this.department = department;
        this.hireDate = hireDate;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    /** 퇴사 처리. 물리 삭제 대신 퇴사일을 남기고 비활성화한다. */
    public void resign(LocalDate resignDate) {
        this.resignDate = resignDate;
        this.active = false;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /* ── 기본 배송지 ─────────────────────────────────────── */

    /** 우편번호와 주소가 모두 있어야 쓸 수 있는 값으로 본다. */
    public boolean hasDefaultAddress() {
        return defaultZipcode != null && !defaultZipcode.isBlank()
                && defaultAddress != null && !defaultAddress.isBlank();
    }

    public void changeDefaultAddress(String zipcode, String address, String detail) {
        this.defaultZipcode = zipcode;
        this.defaultAddress = address;
        this.defaultAddressDetail = detail;
    }

    public void clearDefaultAddress() {
        this.defaultZipcode = null;
        this.defaultAddress = null;
        this.defaultAddressDetail = null;
    }

    /* ── PIN ─────────────────────────────────────────────── */

    /** 발급·초기화. 받은 값은 이미 해시된 것이어야 한다. */
    public void assignPin(String hash, boolean changeRequired) {
        this.pinHash = hash;
        this.pinChangeRequired = changeRequired;
        this.pinFailCount = 0;
        this.pinLockedUntil = null;
    }

    /** 역할이 EMPLOYEE 로 내려갈 때. 다시 관리자가 되면 새로 발급받는다. */
    public void clearPin() {
        this.pinHash = null;
        this.pinChangeRequired = false;
        this.pinFailCount = 0;
        this.pinLockedUntil = null;
    }

    public boolean hasPin() {
        return pinHash != null && !pinHash.isBlank();
    }

    public boolean isPinLocked() {
        return pinLockedUntil != null && LocalDateTime.now().isBefore(pinLockedUntil);
    }

    /** 실패 누적. 한계에 닿으면 잠그고 횟수를 되돌린다. */
    public void recordPinFailure(int limit, java.time.Duration lockFor) {
        this.pinFailCount++;
        if (this.pinFailCount >= limit) {
            this.pinFailCount = 0;
            this.pinLockedUntil = LocalDateTime.now().plus(lockFor);
        }
    }

    public void recordPinSuccess() {
        this.pinFailCount = 0;
        this.pinLockedUntil = null;
    }
}
