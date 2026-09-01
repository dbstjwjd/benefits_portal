-- 관리자 폼 관리 화면 확인용 데이터
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/form-admin-demo.sql
--
-- sample-form.sql 을 먼저 넣은 상태에서 이어서 실행합니다.
-- 목록의 상태 배지와 요약 카드를 모두 보려고 아래 경우를 하나씩 만듭니다.
--   마감 임박(D-3 이내) / 이번 달 마감 / 부서 지정 / 구분 지정 / 작성 중 / 마감

DELETE FROM form_target_department WHERE form_id >= 100;
DELETE FROM response WHERE form_id >= 100;
DELETE FROM form WHERE id >= 100;

-- ── 폼 ────────────────────────────────────────────────────────────
INSERT INTO form (id, created_at, updated_at, title, description, target, start_at, end_at, status, created_by) VALUES
-- 100: 개발팀·디자인팀 대상, 이틀 뒤 마감  → 마감 임박 + 이번 달 마감
(100, NOW(), NOW(), '하반기 워크숍 참석 조사', '워크숍 참석 여부를 알려주세요.',
 'ALL', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 23 HOUR, 'OPEN', '20240002'),
-- 101: 간접직 전체, 이번 달 말 마감      → 이번 달 마감
(101, NOW(), NOW(), '간접직 건강검진 일정 조사', '검진 희망 주간을 골라주세요.',
 'INDIRECT', DATE_SUB(NOW(), INTERVAL 3 DAY), LAST_DAY(CURDATE()) + INTERVAL 23 HOUR, 'OPEN', '20240002'),
-- 102: 직접직 + 개발팀, 다음 달 마감     → 진행 중만
(102, NOW(), NOW(), '개발팀 장비 교체 신청', '교체가 필요한 장비를 적어주세요.',
 'DIRECT', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 'OPEN', '20240002'),
-- 103: 아직 열지 않은 폼                → 전체 필터에서만 '작성 중'
(103, NOW(), NOW(), '사내 동호회 지원 신청', '아직 준비 중인 폼입니다.',
 'ALL', NULL, DATE_ADD(CURDATE(), INTERVAL 60 DAY), 'DRAFT', '20240002'),
-- 104: 지난달 마감                      → 마감 필터
(104, NOW(), NOW(), '상반기 만족도 조사', '지난 분기 만족도 조사입니다.',
 'ALL', DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'CLOSED', '20240002');

-- ── 대상 부서 (비어 있으면 전체 부서) ─────────────────────────────
INSERT INTO form_target_department (form_id, department_id) VALUES
(100, 1),   -- 개발팀
(100, 3),   -- 디자인팀
(102, 1);   -- 개발팀

-- ── 응답 (응답률 막대 확인용) ─────────────────────────────────────
-- 100 은 대상(개발팀+디자인팀) 중 일부만, 104 는 대부분이 응답한 모양으로 만든다.
INSERT INTO response (created_at, updated_at, emp_no, form_id)
SELECT NOW(), NOW(), e.emp_no, 100
FROM employee e
WHERE e.active = 1 AND e.department_id IN (1, 3)
ORDER BY e.emp_no
LIMIT 2;

INSERT INTO response (created_at, updated_at, emp_no, form_id)
SELECT NOW(), NOW(), e.emp_no, 104
FROM employee e
WHERE e.active = 1
ORDER BY e.emp_no
LIMIT 20;

SELECT id, title, target, status, DATE(end_at) AS end_day FROM form ORDER BY id;
