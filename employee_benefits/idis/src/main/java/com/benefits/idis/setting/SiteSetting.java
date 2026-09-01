package com.benefits.idis.setting;

import com.benefits.idis.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화면 문구처럼 코드 배포 없이 바꿔야 하는 값을 담는 키-값 설정.
 * 값이 길 수 있어(JSON 등) TEXT 로 둔다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SiteSetting extends BaseEntity {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    public void change(String value) {
        this.value = value;
    }
}
