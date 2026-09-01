package com.benefits.idis.setting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
    List<SiteSetting> findByKeyIn(List<String> keys);
}
