package com.benefits.idis.auth;

import com.benefits.idis.employee.Employee;

import java.io.Serializable;

/**
 * 세션에 담기는 로그인 정보. 화면 표시에 필요한 최소한만 둔다.
 * 권한·재직 여부·부서는 바뀔 수 있으므로 세션이 아니라 DB 를 다시 본다.
 */
public record LoginUser(String empNo, String name) implements Serializable {

    public static LoginUser from(Employee employee) {
        return new LoginUser(employee.getEmpNo(), employee.getName());
    }
}
