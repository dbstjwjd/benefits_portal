-- 응답 현황 화면 확인용 데이터
--
-- 실행:
--   "C:\Program Files\MySQL\MySQL Server 26.7\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 idis < seed/response-demo.sql
--
-- sample-form.sql 의 1번 폼(추석 선물 신청)에 응답을 채운다.
-- 단일선택·다중선택·단답·주소가 모두 들어 있어 통계 탭과 엑셀을 한 번에 볼 수 있다.
-- 일부러 전원이 아니라 일부만 응답하게 두어 미응답 필터도 확인할 수 있다.

DELETE ac FROM answer_choice ac
  JOIN answer a ON a.id = ac.answer_id
  JOIN response r ON r.id = a.response_id
 WHERE r.form_id = 1;
DELETE a FROM answer a JOIN response r ON r.id = a.response_id WHERE r.form_id = 1;
DELETE FROM response WHERE form_id = 1;

-- ── 응답 ──────────────────────────────────────────────────
-- 재직자 24명 중 앞에서 16명만 응답 (미응답 8명)
INSERT INTO response (created_at, updated_at, emp_no, form_id, edited_at)
SELECT
    TIMESTAMP('2026-08-20') + INTERVAL (ROW_NUMBER() OVER (ORDER BY e.emp_no)) HOUR,
    NOW(), e.emp_no, 1,
    -- 3명마다 한 번은 수정한 것으로 둔다
    CASE WHEN ROW_NUMBER() OVER (ORDER BY e.emp_no) % 3 = 0
         THEN TIMESTAMP('2026-08-22') ELSE NULL END
FROM employee e
WHERE e.active = 1
ORDER BY e.emp_no
LIMIT 16;

-- ── 단일선택 (질문 1) ─────────────────────────────────────
INSERT INTO answer (response_id, question_id, value)
SELECT r.id, 1, NULL FROM response r WHERE r.form_id = 1;

-- 한우 8 / 과일 5 / 건강식품 3 으로 갈리게 넣는다
INSERT INTO answer_choice (answer_id, choice_id)
SELECT a.id,
       CASE WHEN t.seq <= 8 THEN 1 WHEN t.seq <= 13 THEN 2 ELSE 3 END
FROM answer a
JOIN response r ON r.id = a.response_id
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq FROM response WHERE form_id = 1) t
  ON t.id = r.id
WHERE a.question_id = 1;

-- ── 단답 (질문 2, 3) ──────────────────────────────────────
INSERT INTO answer (response_id, question_id, value)
SELECT r.id, 2, e.name FROM response r JOIN employee e ON e.emp_no = r.emp_no WHERE r.form_id = 1;

INSERT INTO answer (response_id, question_id, value)
SELECT r.id, 3, e.phone FROM response r JOIN employee e ON e.emp_no = r.emp_no WHERE r.form_id = 1;

-- ── 주소 (질문 4) ─────────────────────────────────────────
INSERT INTO answer (response_id, question_id, value)
SELECT r.id, 4,
       CONCAT('{"zipcode":"0623', t.seq % 10,
              '","address":"서울 강남구 테헤란로 ', 100 + t.seq,
              '","detail":"', t.seq, '층"}')
FROM response r
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq FROM response WHERE form_id = 1) t
  ON t.id = r.id
WHERE r.form_id = 1;

-- ── 다중선택 (질문 9) ─────────────────────────────────────
-- 절반 정도만 이 질문에 답하고, 답한 사람은 1~2개를 고른다
INSERT INTO answer (response_id, question_id, value)
SELECT r.id, 9, NULL
FROM response r
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq FROM response WHERE form_id = 1) t
  ON t.id = r.id
WHERE r.form_id = 1 AND t.seq <= 10;

INSERT INTO answer_choice (answer_id, choice_id)
SELECT a.id, 11 FROM answer a WHERE a.question_id = 9;

INSERT INTO answer_choice (answer_id, choice_id)
SELECT a.id, 13
FROM answer a
JOIN response r ON r.id = a.response_id
JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS seq FROM response WHERE form_id = 1) t
  ON t.id = r.id
WHERE a.question_id = 9 AND t.seq % 2 = 0;

SELECT
    (SELECT COUNT(*) FROM response WHERE form_id = 1) AS 응답,
    (SELECT COUNT(*) FROM employee WHERE active = 1)  AS 재직,
    (SELECT COUNT(*) FROM answer a JOIN response r ON r.id = a.response_id WHERE r.form_id = 1) AS 답변;
