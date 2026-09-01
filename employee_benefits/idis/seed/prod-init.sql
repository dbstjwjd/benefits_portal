-- 운영 최초 1회만 실행하는 초기 데이터.
-- 첫 관리자 계정과 화면 문구만 넣는다. 데모 데이터는 들어 있지 않다.
--
-- ★ seed/employee.sql 과 헷갈리지 말 것. 그쪽은 로컬 개발용이고 PIN 평문이 000000 이다.
--   운영에 들어가야 하는 것은 schema.sql 과 이 파일 둘뿐이다.
--
--   mysql -u idis -p --default-character-set=utf8mb4 idis < seed/prod-init.sql
--
-- ★ 실행 전에 <> 로 표시된 값을 전부 실제 값으로 바꿀 것.
--   바꾸지 않고 실행하면 <부서1> 같은 이름이 그대로 DB 에 들어간다.
--
-- 전화번호는 로그인 키다. 하이픈을 넣어도 되고 빼도 되지만 실제 번호여야 한다.
-- employee.phone 에 UNIQUE 가 걸려 있어 같은 번호를 두 번 넣을 수 없다.
--
-- 스키마는 애플리케이션이 만들지 않는다(ddl-auto=validate).
-- 최초 배포에서는 setup.md 의 '스키마 만들기' 순서를 먼저 따른다.
--
-- 전체가 하나의 트랜잭션이다. 값이 잘못돼 중간에 멈추면 아무것도 안 들어가므로,
-- 값을 고쳐서 그대로 다시 실행하면 된다.
-- 반대로 한 번 성공한 뒤에는 다시 실행할 수 없다.
-- department.name 과 site_setting.setting_key 에 UNIQUE·PK 가 걸려 있다.

-- 값이 잘못되면 조용히 넘어가지 않고 그 자리에서 멈추게 한다.
SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',STRICT_ALL_TABLES');

-- 전부 들어가거나 아무것도 안 들어가거나 둘 중 하나로 만든다.
-- 중간에 멈추면 롤백되므로, 값을 고쳐서 그냥 다시 실행하면 된다.
START TRANSACTION;

-- ── 부서 ──────────────────────────────────────────────────
-- 실제 부서 이름으로 바꾸고, 필요한 만큼 줄을 늘리거나 줄인다.
INSERT INTO department (created_at, updated_at, name) VALUES
    (NOW(), NOW(), '생산운영팀'),
    (NOW(), NOW(), '생산기술팀'),
    (NOW(), NOW(), '품질보증팀'),
    (NOW(), NOW(), '자재외주관리팀'),
    (NOW(), NOW(), 'CS팀');

-- ── 첫 관리자 ─────────────────────────────────────────────
-- 이 계정으로 로그인한 뒤 나머지 직원은 화면에서 엑셀로 올린다.
--
-- 아래 <관리자부서> 는 **위 INSERT 에 적은 부서 이름 중 하나와 정확히 같아야 한다.**
SET @admin_dept = '생산운영팀';
SET @admin_dept_id = (SELECT id FROM department WHERE name = @admin_dept);

-- 부서를 못 찾으면 여기서 스크립트를 멈춘다.
-- employee.department_id 는 NULL 을 허용하므로, 이 검사가 없으면
-- 부서 없는 관리자가 오류 없이 조용히 만들어진다.
--   멈추면 나오는 메시지: Column 'id' cannot be null
--   → @admin_dept 값을 위 부서 이름과 맞추면 된다.
CREATE TEMPORARY TABLE _admin_dept_guard (id bigint NOT NULL);
INSERT INTO _admin_dept_guard (id) VALUES (@admin_dept_id);
DROP TEMPORARY TABLE _admin_dept_guard;

-- type: DIRECT(직접직) 또는 INDIRECT(간접직)
-- super_admin = 1 : 역할을 바꿀 수 있는 유일한 계정이고, 남이 건드릴 수 없다.
--                   화면에는 드러나지 않으며 애플리케이션에서 바꿀 수 없다.
--
-- ★ pin_hash : 관리자는 이름+전화번호만으로는 못 들어온다. PIN 6자리가 더 필요하다.
--    아래 <PIN해시> 는 원문 PIN 이 아니라 BCrypt 해시다. 서버에서 이렇게 만든다:
--
--      sudo apt install -y apache2-utils
--      htpasswd -bnBC 10 "" 123456 | tr -d ':
'
--
--    출력된 $2y$10$... 를 통째로 넣는다. ($2a$/$2b$/$2y$ 모두 인식한다)
--    '123456' 자리에 실제로 쓸 6자리를 넣되, 이 값은 첫 로그인에서 바로 바뀐다.
--    pin_change_required = 1 이라 본인이 새 PIN 을 정해야 화면에 들어갈 수 있다.
INSERT INTO employee
    (created_at, updated_at, emp_no, name, phone, type, role, active, super_admin,
     pin_hash, pin_change_required, pin_fail_count, hire_date, department_id)
VALUES
    (NOW(), NOW(),
     '20250511',
     '정윤서',
     '010-2461-6781',
     'DIRECT',
     'ADMIN',
     1,
     1,
     '<PIN해시>',
     1,
     0,
     '2025-05-20',
     @admin_dept_id);

-- ── 화면 문구 ─────────────────────────────────────────────
-- 로그인 안내 문구와 문의 모달. 넣은 뒤에는 관리자 화면 > 설정에서 바꿀 수 있다.
-- 아래 5건이 한 문장이다. 마지막 튜플 뒤의 ; 를 지우지 말 것.
INSERT INTO site_setting (created_at, updated_at, setting_key, value) VALUES
    (NOW(), NOW(), 'login_notice',
     'IDIS 복리후생 시스템입니다.\n이름과 전화번호로 로그인해 주세요.'),

    (NOW(), NOW(), 'contact_title', '문의하기'),

    (NOW(), NOW(), 'contact_intro', '아래 담당자에게 문의해주세요'),

    (NOW(), NOW(), 'contact_footnote', '업무 시간(08:30 ~ 17:30) 중에 연락 또는 방문 부탁드립니다.'),

    -- 담당자 목록. 설정 화면에서 편집하므로 지금은 한 명만 넣어도 된다.
        (NOW(), NOW(), 'contact_json',
     '[{"name":"김혜지","role":"복리후생 문의","location":"2층 DNVR 사무실","extension":""},
       {"name":"정윤서","role":"사이트 이용 문의","location":"4층 IPC 사무실","extension":""}]');
COMMIT;

-- ── 확인 ──────────────────────────────────────────────────
-- dept_check 에 '★' 가 뜨면 관리자에게 부서가 안 붙은 것이다.
SELECT e.emp_no, e.name, d.name AS department, e.role, e.active, e.super_admin,
       CASE WHEN e.department_id IS NULL
            THEN '★ 부서 없음 - 확인 필요' ELSE 'OK' END AS dept_check,
       CASE WHEN e.pin_hash LIKE '$2%' THEN 'OK'
            ELSE '★ PIN 해시 아님 - 관리자가 로그인할 수 없다' END AS pin_check
FROM employee e
LEFT JOIN department d ON d.id = e.department_id;

SELECT setting_key, LEFT(value, 40) AS value_head FROM site_setting ORDER BY setting_key;

-- 문구가 5건이 아니면 위 INSERT 가 중간에 끊긴 것이다.
SELECT CASE WHEN COUNT(*) = 5 THEN CONCAT('OK - 문구 ', COUNT(*), '건')
            ELSE CONCAT('★ 문구가 ', COUNT(*), '건뿐입니다. 5건이어야 합니다') END AS setting_check
FROM site_setting;

-- 바꾸지 않고 남은 placeholder 가 있으면 여기서 잡힌다.
SELECT CONCAT('★ 바꾸지 않은 값 ', COUNT(*), '건') AS placeholder_check
FROM (
    SELECT name AS v FROM department
    UNION ALL SELECT emp_no FROM employee
    UNION ALL SELECT name FROM employee
    UNION ALL SELECT phone FROM employee
    UNION ALL SELECT value FROM site_setting
) t
WHERE v LIKE '%<%>%'
HAVING COUNT(*) > 0;
