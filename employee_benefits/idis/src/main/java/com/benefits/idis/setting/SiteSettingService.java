package com.benefits.idis.setting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSettingService {

    public static final String LOGIN_NOTICE = "login_notice";

    public static final String CONTACT_TITLE = "contact_title";
    public static final String CONTACT_INTRO = "contact_intro";
    public static final String CONTACT_JSON = "contact_json";
    public static final String CONTACT_FOOTNOTE = "contact_footnote";

    private static final List<String> CONTACT_KEYS =
            List.of(CONTACT_TITLE, CONTACT_INTRO, CONTACT_JSON, CONTACT_FOOTNOTE);

    private final SiteSettingRepository siteSettingRepository;
    private final ObjectMapper objectMapper;

    /** 문의 모달에 필요한 설정을 한 번에 읽는다. 없는 키는 map 에 들어가지 않는다. */
    public Map<String, String> contactSettings() {
        Map<String, String> settings = new HashMap<>();
        for (SiteSetting setting : siteSettingRepository.findByKeyIn(CONTACT_KEYS)) {
            if (setting.getValue() != null) {
                settings.put(setting.getKey(), setting.getValue());
            }
        }
        return settings;
    }

    /**
     * contact_json 을 담당자 목록으로 파싱한다.
     * 키가 없거나 JSON 이 깨져 있어도 화면은 떠야 하므로 빈 목록으로 떨어뜨린다.
     */
    public List<ContactPerson> contactPersons(Map<String, String> settings) {
        String json = settings.get(CONTACT_JSON);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<ContactPerson> parsed = objectMapper.readValue(json, new TypeReference<>() {
            });
            return parsed == null ? List.of() : parsed;
        } catch (JacksonException e) {
            log.warn("{} 파싱 실패. 담당자 목록을 비워 둡니다.", CONTACT_JSON, e);
            return List.of();
        }
    }

    /** 설정이 비어 있어도 화면 문구가 사라지지 않도록 기본값을 둔다. */
    public String textOr(Map<String, String> settings, String key, String fallback) {
        return settings.getOrDefault(key, fallback);
    }

    /** 키 하나를 읽는다. 없으면 기본값. */
    public String text(String key, String fallback) {
        return siteSettingRepository.findById(key)
                .map(SiteSetting::getValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    /** 여러 키를 한 번에 저장한다. 없으면 새로 만들고 있으면 값만 바꾼다. */
    @Transactional
    public void save(Map<String, String> values) {
        values.forEach((key, value) -> siteSettingRepository.findById(key)
                .ifPresentOrElse(
                        setting -> setting.change(value),
                        () -> siteSettingRepository.save(
                                SiteSetting.builder().key(key).value(value).build())));
    }

    /** 담당자 목록을 contact_json 문자열로 만든다. */
    public String toJson(List<ContactPerson> persons) {
        return objectMapper.writeValueAsString(persons);
    }
}
