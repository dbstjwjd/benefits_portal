package com.benefits.idis.admin;

import com.benefits.idis.employee.EmployeeType;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * 직원 목록 검색 조건. 값이 없으면 그 조건은 무시한다.
 * status 는 재직(active) / 퇴사(resigned) / 전체(all) 세 가지이고 기본은 재직이다.
 */
public record EmployeeSearch(
        String name,
        Long departmentId,
        EmployeeType type,
        String status,
        int page,
        String sort,
        String dir
) {
    public static final String ACTIVE = "active";
    public static final String RESIGNED = "resigned";
    public static final String ALL = "all";

    public static final int PAGE_SIZE = 20;

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    /**
     * 정렬할 수 있는 칸만 둔다. 값은 엔티티 기준 프로퍼티 경로다.
     * 화면에서 온 문자열을 그대로 쿼리에 넣지 않으려고 여기서 한 번 거른다.
     */
    private static final Map<String, String> SORTABLE = Map.of(
            "empNo", "empNo",
            "name", "name",
            "dept", "department.name",
            "type", "type",
            "hireDate", "hireDate");

    public EmployeeSearch {
        name = (name == null || name.isBlank()) ? null : name.strip();
        status = (status == null || status.isBlank()) ? ACTIVE : status;
        page = Math.max(page, 0);
        // Map.of 는 null 키를 거부하므로 먼저 걸러 낸다
        sort = (sort != null && SORTABLE.containsKey(sort)) ? sort : null;
        dir = DESC.equals(dir) ? DESC : (sort == null ? null : ASC);
    }

    /**
     * 정렬을 안 걸면 이름 오름차순. 걸었을 때도 이름을 뒤에 붙여
     * 값이 같은 행의 순서가 페이지마다 흔들리지 않게 한다.
     */
    public Sort sortOrder() {
        if (sort == null) {
            return Sort.by(Sort.Order.asc("name"));
        }
        Sort.Order order = DESC.equals(dir)
                ? Sort.Order.desc(SORTABLE.get(sort))
                : Sort.Order.asc(SORTABLE.get(sort));
        return "name".equals(sort) ? Sort.by(order) : Sort.by(order, Sort.Order.asc("name"));
    }

    /** 이 칸의 현재 방향. 정렬 중이 아니면 null 이다. */
    public String dirOf(String column) {
        return column.equals(sort) ? dir : null;
    }

    /**
     * 헤더에 붙일 표시.
     * 정렬 중이 아닐 때도 옅게 ⇅ 를 두어야 '누를 수 있다'는 것이 보인다.
     */
    public String arrowOf(String column) {
        String current = dirOf(column);
        if (current == null) {
            return "⇅";
        }
        return ASC.equals(current) ? "▲" : "▼";
    }

    /** 헤더를 눌렀을 때 갈 정렬 칸. 내림차순이었으면 해제(null)한다. */
    public String nextSort(String column) {
        return column.equals(sort) && DESC.equals(dir) ? null : column;
    }

    /** 오름차순 → 내림차순 → 해제 순환. */
    public String nextDir(String column) {
        if (!column.equals(sort)) {
            return ASC;
        }
        return ASC.equals(dir) ? DESC : null;
    }

    /** 검색 쿼리에 넘길 active 값. 전체면 null 이라 조건이 빠진다. */
    public Boolean activeFlag() {
        return switch (status) {
            case RESIGNED -> Boolean.FALSE;
            case ALL -> null;
            default -> Boolean.TRUE;
        };
    }

    /** 검색·필터를 하나라도 걸었는지. 빈 화면 문구를 고르는 데 쓴다. */
    public boolean hasFilter() {
        return name != null || departmentId != null || type != null || !ACTIVE.equals(status);
    }
}
