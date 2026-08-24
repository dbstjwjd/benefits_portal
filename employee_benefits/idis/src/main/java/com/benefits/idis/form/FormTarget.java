package com.benefits.idis.form;

import com.benefits.idis.employee.EmployeeType;

public enum FormTarget {
    DIRECT, INDIRECT, ALL;

    public boolean includes(EmployeeType type) {
        return this == ALL || this.name().equals(type.name());
    }
}
