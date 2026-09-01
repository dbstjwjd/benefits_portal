package com.benefits.idis.admin;

import com.benefits.idis.setting.ContactPerson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 문의 담당자 설정 입력. 담당자 행은 화면에서 만든 순서대로 들어온다. */
@Getter
@Setter
@NoArgsConstructor
public class ContactForm {

    private String title;
    private String intro;
    private String footnote;

    private List<PersonForm> persons = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PersonForm {
        private String name;
        private String role;
        private String location;
        private String extension;

        boolean blank() {
            return isBlank(name) && isBlank(role) && isBlank(location) && isBlank(extension);
        }

        ContactPerson toPerson() {
            return new ContactPerson(strip(name), strip(role), strip(location), strip(extension));
        }
    }

    /** 이름이 없는 행은 화면에서 빈 줄로 남겨둔 것으로 보고 버린다. */
    public List<ContactPerson> people() {
        return persons.stream()
                .filter(person -> !person.blank())
                .map(PersonForm::toPerson)
                .toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }
}
