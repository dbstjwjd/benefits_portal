-- 관리자 화면 확인용 직원 데이터
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/employee-demo.sql
--
-- seed/employee.sql (홍길동·김관리) 을 먼저 실행한 뒤 사용한다.
-- 페이지네이션·필터·퇴사 상태를 확인할 수 있도록 부서 7개 / 직원 24명을 넣는다.
-- 이번 달 입사·퇴사 카드를 확인하려고 일부는 현재 월 날짜를 쓴다.

INSERT INTO department (id, name, created_at, updated_at) VALUES
(3, '디자인팀',     NOW(6), NOW(6)),
(4, '영업팀',       NOW(6), NOW(6)),
(5, '인사팀',       NOW(6), NOW(6)),
(6, '마케팅팀',     NOW(6), NOW(6)),
(7, '경영기획팀',   NOW(6), NOW(6))
AS new
ON DUPLICATE KEY UPDATE name = new.name, updated_at = NOW(6);

INSERT INTO employee
    (emp_no, name, phone, type, role, active, hire_date, resign_date, department_id, created_at, updated_at,
     super_admin, pin_hash, pin_change_required, pin_fail_count, pin_locked_until) VALUES
('20240003', '이영희', '010-2345-6789', 'DIRECT',   'EMPLOYEE', b'1', '2021-05-12', NULL, 3, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240004', '김철수', '010-3456-7890', 'DIRECT',   'EMPLOYEE', b'1', '2020-11-01', NULL, 4, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240005', '박민수', '010-4567-8901', 'INDIRECT', 'EMPLOYEE', b'1', '2019-04-15', NULL, 5, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240006', '최지원', '010-5678-9012', 'INDIRECT', 'EMPLOYEE', b'1', '2022-01-10', NULL, 6, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240007', '정다은', '010-6789-0123', 'DIRECT',   'EMPLOYEE', b'1', '2022-07-01', NULL, 1, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240008', '강현우', '010-7890-1234', 'DIRECT',   'EMPLOYEE', b'1', '2020-08-20', NULL, 4, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240009', '윤소현', '010-8901-2345', 'DIRECT',   'EMPLOYEE', b'1', '2021-10-05', NULL, 3, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240010', '임재덕', '010-9012-3456', 'INDIRECT', 'EMPLOYEE', b'1', '2018-09-01', NULL, 7, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240011', '조유리', '010-1122-3344', 'INDIRECT', 'EMPLOYEE', b'1', '2023-02-15', NULL, 5, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240012', '한서윤', '010-2233-4455', 'DIRECT',   'EMPLOYEE', b'1', '2021-12-01', NULL, 1, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240013', '서혜진', '010-3344-5566', 'DIRECT',   'EMPLOYEE', b'1', '2022-03-22', NULL, 3, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240014', '이준혁', '010-4455-6677', 'INDIRECT', 'EMPLOYEE', b'1', '2020-02-10', NULL, 5, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240015', '김나영', '010-5566-7788', 'INDIRECT', 'EMPLOYEE', b'1', '2023-01-05', NULL, 6, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240016', '오승현', '010-6677-8899', 'DIRECT',   'EMPLOYEE', b'1', '2021-08-11', NULL, 4, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240017', '문가영', '010-7788-9900', 'DIRECT',   'EMPLOYEE', b'1', '2019-06-03', NULL, 1, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240018', '배진호', '010-8899-0011', 'INDIRECT', 'EMPLOYEE', b'1', '2020-05-18', NULL, 7, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240019', '신유진', '010-9900-1122', 'DIRECT',   'EMPLOYEE', b'1', '2022-11-07', NULL, 3, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240020', '홍석민', '010-1010-2020', 'DIRECT',   'EMPLOYEE', b'1', '2018-03-26', NULL, 4, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240021', '권도현', '010-3030-4040', 'INDIRECT', 'EMPLOYEE', b'1', '2023-09-11', NULL, 6, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240022', '남지수', '010-5050-6060', 'DIRECT',   'EMPLOYEE', b'1', '2024-02-19', NULL, 1, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
-- 이번 달 입사 2명
('20240023', '유하늘', '010-7070-8080', 'DIRECT',   'EMPLOYEE', b'1', DATE_FORMAT(NOW(), '%Y-%m-05'), NULL, 1, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240024', '전소민', '010-9090-1212', 'INDIRECT', 'EMPLOYEE', b'1', DATE_FORMAT(NOW(), '%Y-%m-12'), NULL, 5, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
-- 퇴사자 2명 (한 명은 이번 달 퇴사)
('20240025', '차은우', '010-1313-1414', 'DIRECT',   'EMPLOYEE', b'0', '2019-01-07', '2025-11-30', 4, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL),
('20240026', '류지훈', '010-1515-1616', 'INDIRECT', 'EMPLOYEE', b'0', '2020-04-01', DATE_FORMAT(NOW(), '%Y-%m-08'), 7, NOW(6), NOW(6), b'0', NULL, b'0', 0, NULL)
AS new
ON DUPLICATE KEY UPDATE
    name          = new.name,
    phone         = new.phone,
    type          = new.type,
    role          = new.role,
    active        = new.active,
    super_admin         = new.super_admin,
    pin_hash            = new.pin_hash,
    pin_change_required = new.pin_change_required,
    pin_fail_count      = new.pin_fail_count,
    pin_locked_until    = new.pin_locked_until,
    hire_date     = new.hire_date,
    resign_date   = new.resign_date,
    department_id = new.department_id,
    updated_at    = NOW(6);
