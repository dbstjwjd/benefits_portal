-- 개발용 샘플 폼 데이터
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/sample-form.sql
--
-- 주의: 이미 응답(response)이 쌓인 뒤에 다시 실행하면 FK 제약으로 실패합니다.
--       그때는 response 관련 데이터를 먼저 정리하세요.
--
-- created_by 는 김관리(20240002, ADMIN) 기준입니다.

DELETE FROM choice;
DELETE FROM question;
DELETE FROM form;

ALTER TABLE form AUTO_INCREMENT = 1;
ALTER TABLE question AUTO_INCREMENT = 1;
ALTER TABLE choice AUTO_INCREMENT = 1;

-- ── 폼 ────────────────────────────────────────────────────────────
-- 1: 전체 대상, 신청 가능          → 홍길동/김관리 모두 노출
-- 2: 전체 대상, 신청 가능          → 홍길동/김관리 모두 노출
-- 3: 직접직 대상, 신청 가능        → 홍길동(DIRECT)만 노출
-- 4: 전체 대상이지만 CLOSED        → 아무에게도 노출되지 않음
INSERT INTO form (id, created_at, updated_at, title, description, target, status, start_at, end_at, created_by) VALUES
(1, NOW(6), NOW(6), '2026년 추석 선물 신청',
 '추석 선물을 선택하고 배송받을 주소를 입력해주세요. 신청 후에도 마감 전까지 수정할 수 있습니다.',
 'ALL', 'OPEN', '2026-08-01 00:00:00', '2026-09-15 23:59:59', '20240002'),
(2, NOW(6), NOW(6), '2026년 생일 케이크 신청',
 '생일이 있는 달에 받으실 케이크를 선택해주세요.',
 'ALL', 'OPEN', '2026-01-01 00:00:00', '2026-12-31 23:59:59', '20240002'),
(3, NOW(6), NOW(6), 'IDIS 창립기념품 신청',
 '창립기념일 기념품을 선택해주세요.',
 'DIRECT', 'OPEN', '2026-08-10 00:00:00', '2026-10-31 23:59:59', '20240002'),
(4, NOW(6), NOW(6), '2026년 설 명절 선물 신청',
 '신청이 마감되었습니다.',
 'ALL', 'CLOSED', '2026-01-05 00:00:00', '2026-02-10 23:59:59', '20240002');

-- ── 1번 폼 질문 ───────────────────────────────────────────────────
INSERT INTO question (id, created_at, updated_at, form_id, type, title, required, sort_order, config) VALUES
(1, NOW(6), NOW(6), 1, 'SINGLE_CHOICE', '받고 싶은 추석 선물을 선택해주세요', b'1', 1, NULL),
(2, NOW(6), NOW(6), 1, 'SHORT_TEXT',    '수령인 이름',                       b'1', 2, NULL),
(3, NOW(6), NOW(6), 1, 'SHORT_TEXT',    '수령인 연락처',                     b'1', 3, NULL),
(4, NOW(6), NOW(6), 1, 'ADDRESS',       '배송받을 주소',                     b'1', 4, NULL);

INSERT INTO choice (id, question_id, content, image_path, sort_order) VALUES
(1, 1, '한우 정육 세트',      NULL, 1),
(2, 1, '프리미엄 과일 세트',  NULL, 2),
(3, 1, '건강식품 세트',       NULL, 3);

-- ── 2번 폼 질문 ───────────────────────────────────────────────────
INSERT INTO question (id, created_at, updated_at, form_id, type, title, required, sort_order, config) VALUES
(5, NOW(6), NOW(6), 2, 'SINGLE_CHOICE', '케이크 종류를 선택해주세요', b'1', 1, NULL),
(6, NOW(6), NOW(6), 2, 'SHORT_TEXT',    '알레르기 등 특이사항',      b'0', 2, NULL);

INSERT INTO choice (id, question_id, content, image_path, sort_order) VALUES
(4, 5, '초코 케이크',   NULL, 1),
(5, 5, '생크림 케이크', NULL, 2),
(6, 5, '치즈 케이크',   NULL, 3);

-- ── 3번 폼 질문 ───────────────────────────────────────────────────
INSERT INTO question (id, created_at, updated_at, form_id, type, title, required, sort_order, config) VALUES
(7, NOW(6), NOW(6), 3, 'SINGLE_CHOICE', '기념품을 선택해주세요', b'1', 1, NULL);

INSERT INTO choice (id, question_id, content, image_path, sort_order) VALUES
(7, 7, '텀블러',       NULL, 1),
(8, 7, '블루투스 키보드', NULL, 2);

-- ── 4번 폼 질문 ───────────────────────────────────────────────────
INSERT INTO question (id, created_at, updated_at, form_id, type, title, required, sort_order, config) VALUES
(8, NOW(6), NOW(6), 4, 'SINGLE_CHOICE', '받고 싶은 설 선물을 선택해주세요', b'1', 1, NULL);

INSERT INTO choice (id, question_id, content, image_path, sort_order) VALUES
(9, 8, '갈비 세트', NULL, 1),
(10, 8, '건과류 세트', NULL, 2);
