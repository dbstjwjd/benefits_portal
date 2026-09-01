-- 사이트 설정 (site_setting)
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/site-setting.sql
--
-- 로그인 화면의 "관리자 문의하기" 모달 문구와 담당자 목록.
-- contact_json 의 이름/위치/내선은 placeholder 이므로 실제 값으로 교체해야 한다.
--
-- 재실행해도 안전하도록 UPSERT 로 넣는다.

INSERT INTO site_setting (setting_key, value, created_at, updated_at) VALUES
('contact_title',
 '사이트 이용 문의',
 NOW(6), NOW(6)),

('contact_intro',
 '로그인이 안 되거나 사이트 이용에 문제가 있으면 아래 담당자에게 방문 또는 내선으로 문의해 주세요.',
 NOW(6), NOW(6)),

-- TODO: 이름 / 위치 / 내선을 실제 담당자 정보로 교체
('contact_json',
 '[{"name":"담당자 1","role":"사이트 운영","location":"본관 3층 개발팀","extension":"000"},
   {"name":"담당자 2","role":"계정 등록","location":"본관 2층 경영지원팀","extension":"000"}]',
 NOW(6), NOW(6)),

('contact_footnote',
 '업무 시간(08:30 ~ 17:30) 중에 연락 부탁드립니다.',
 NOW(6), NOW(6))

AS new
ON DUPLICATE KEY UPDATE
    value = new.value,
    updated_at = NOW(6);
