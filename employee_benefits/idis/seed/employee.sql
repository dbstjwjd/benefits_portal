-- 임직원 / 부서 시드
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/employee.sql
--
-- 사번(emp_no)은 PK 라 unique·not null 이 이미 보장된다.
-- 구분은 type 컬럼(DIRECT=직접직 / INDIRECT=간접직).
--
-- 재실행해도 안전하도록 UPSERT 로 넣는다.
--
-- ★ 로컬 개발 전용 파일이다. 운영에는 절대 넣지 않는다.
--   김관리는 super_admin 이고 PIN 해시까지 들어 있어, 이 파일이 운영에 들어가면
--   누구나 아래 번호와 PIN 으로 최고 권한 계정에 로그인할 수 있다.
--   운영 초기 데이터는 seed/prod-init.sql 하나뿐이다.
--
-- 관리자 PIN (개발용 평문): 000000
--   pin_change_required = 0 이라 로컬에서는 변경 화면을 거치지 않고 바로 들어간다.

INSERT INTO department (id, name, created_at, updated_at) VALUES
(1, '개발팀',       NOW(6), NOW(6)),
(2, '경영지원팀',   NOW(6), NOW(6))
AS new
ON DUPLICATE KEY UPDATE name = new.name, updated_at = NOW(6);

INSERT INTO employee
    (emp_no, name, phone, type, role, active, super_admin,
     pin_hash, pin_change_required, pin_fail_count, pin_locked_until,
     hire_date, resign_date, department_id, created_at, updated_at) VALUES
('20240001', '홍길동', '010-1234-5678', 'DIRECT',   'EMPLOYEE', b'1', b'0',
     NULL, b'0', 0, NULL, '2024-03-04', NULL, 1, NOW(6), NOW(6)),
-- PIN 000000
('20240002', '김관리', '010-9876-5432', 'INDIRECT', 'ADMIN',    b'1', b'1',
     '$2a$10$28zk3brKJio8d7qiWkK8d.pKYZKizp9PmZ8kqmnDs.6MFNoMSTDc2', b'0', 0, NULL, '2024-01-02', NULL, 2, NOW(6), NOW(6))
AS new
ON DUPLICATE KEY UPDATE
    name          = new.name,
    phone         = new.phone,
    type          = new.type,
    role          = new.role,
    active        = new.active,
    super_admin   = new.super_admin,
    pin_hash            = new.pin_hash,
    pin_change_required = new.pin_change_required,
    pin_fail_count      = new.pin_fail_count,
    pin_locked_until    = new.pin_locked_until,
    hire_date     = new.hire_date,
    resign_date   = new.resign_date,
    department_id = new.department_id,
    updated_at    = NOW(6);
