# IDIS 직원 복리후생 시스템 — 인수인계

다른 AI/개발자에게 이 프로젝트를 넘길 때 그대로 붙여 넣는 문서.
작성 2026-09-02 기준. 코드에서 직접 확인한 내용만 적었다.

> **붙여 넣기 전에 확인** — 아래에는 운영 서버 IP, 도메인, 로컬 개발용 PIN 평문이 들어 있다.
> 외부 서비스에 올릴 때 문제가 되면 그 부분만 지우고 쓴다.
> DB 비밀번호와 개인키 경로는 애초에 넣지 않았다.

---

## 1. 무엇을 만드는가

사내 임직원이 복리후생 신청(명절 선물, 기념품, 건강검진 등)을 **폼으로 신청**하고,
관리자가 **폼을 만들고 응답을 집계·엑셀로 내려받는** 사내 웹 시스템.

- 사용자(직원): 모바일 우선. 로그인 → 신청 가능한 폼 목록 → 작성/제출 → 신청내역
- 관리자: PC 전용(1024px 미만은 안내 화면). 대시보드·폼 관리·응답 현황·직원 관리·설정

이미 **운영 중**이다. 디자인은 Figma 시안이 있고 "시안 우선"이 원칙.

---

## 2. 기술 스택

| 항목 | 값 |
|---|---|
| Spring Boot | 4.1.0 |
| Java | 21 (Gradle toolchain) |
| 빌드 | Gradle wrapper (`gradlew.bat`) |
| 뷰 | Thymeleaf (SSR). SPA 아님 |
| DB | MySQL 8 (로컬은 MySQL 26.7 클라이언트) |
| ORM | Spring Data JPA / Hibernate |
| 엑셀 | Apache POI 5.3.0 |
| 해시 | `spring-security-crypto` (BCrypt만. Spring Security 전체는 안 씀) |
| JS | **바닐라 JS만.** jQuery·프레임워크 없음 |
| 개발 환경 | Windows + PowerShell 5.1 |

`spring-boot-devtools`가 있어 클래스가 바뀌면 자동 재시작된다.

### Jackson 주의
Jackson 3을 쓴다. import가 **`tools.jackson.*`** 이다 (`com.fasterxml.jackson.databind`가 아님).
단, 애노테이션은 여전히 `com.fasterxml.jackson.annotation.*`.

---

## 3. 패키지 구조

```
com.benefits.idis
├── IdisApplication.java
├── MainController.java
├── admin/      관리자 화면 전체 (컨트롤러·서비스·화면 DTO)
├── auth/       로그인, 관리자 PIN
├── common/     BaseEntity, PhoneFormat
├── config/     WebConfig (인터셉터, 정적 리소스)
├── employee/   Employee, Department, 기본 배송지
├── form/       Form, Question, Choice
├── response/   Response, Answer, AnswerChoice
└── setting/    SiteSetting (화면 문구)
```

엔티티 9개: `Employee` `Department` `Form` `Question` `Choice` `Response` `Answer` `AnswerChoice` `SiteSetting`

컨트롤러 10개: `Main` `Auth` `Form` `DefaultAddress` `Admin` `AdminPin` `EmployeeAdmin` `FormAdmin` `ResponseAdmin` `SettingsAdmin`

템플릿 16개 (`templates/`, 관리자는 `templates/admin/`)
정적 JS 5개 (`static/js/admin-*.js`) — 사용자 화면 JS는 템플릿에 인라인

---

## 4. 도메인 모델과 불변 규칙

### Employee (PK `empNo`)

| 필드 | 설명 |
|---|---|
| `empNo` | 사번. PK, 수정 불가 |
| `name` / `phone` | **이름 + 전화번호가 로그인 키.** `phone`에 UNIQUE |
| `type` | **구분**. `DIRECT`(직접직) / `INDIRECT`(간접직) |
| `role` | `EMPLOYEE` / `ADMIN` |
| `active` / `resignDate` | 퇴사는 물리 삭제 없이 `active=false` + 퇴사일 |
| `superAdmin` | 역할을 바꿀 수 있는 유일한 계정. **시드로만 넣고 앱에서 못 바꿈** |
| `pinHash` `pinChangeRequired` `pinFailCount` `pinLockedUntil` | 관리자 PIN |
| `defaultZipcode` `defaultAddress` `defaultAddressDetail` | 기본 배송지 |

### 반드시 지킬 것 (사용자가 명시한 규칙)

- **`empNo`가 사번, `type`이 구분.** 별도 컬럼 만들지 말 것. 이름 바꾸지 말 것
- **엔티티에 `@Setter` 금지.** 의미 있는 메서드로 상태를 바꾼다 (`resign()`, `changeRole()`, `assignPin()` …)
- 화면 DTO(`~Form`, `~Row`, `~View`)에는 Setter 써도 됨
- **권한·재직 여부·role은 세션이 아니라 DB에서 다시 읽는다** (세션 타임아웃 30일이라 그 사이 바뀔 수 있음)
- 코드 주석은 최소로. "무엇"이 아니라 **"왜"** 를 적는다
- 필터는 네이티브 `<select>` 유지

### QuestionType — 8종이 전부

```
SHORT_TEXT, LONG_TEXT, PHONE,
SINGLE_CHOICE, MULTI_CHOICE, IMAGE_CHOICE,
DATE, ADDRESS
```

> **⚠ 가장 중요한 함정.**
> 자바 enum · 편집 화면의 `<select>` · DB `question.type` ENUM **셋이 항상 같아야 한다.**
> 하나만 늘리면 편집기가 그 질문을 못 그리고, 저장할 때 전송되지 않아 **DB에서 조용히 사라진다.**
> 실제로 이 버그가 있었고(NUMBER/DROPDOWN/SCALE/FILE/SECTION이 enum에만 있었음),
> 재현해보니 질문 3개 중 1개만 남고 선택지·대상 부서까지 날아갔다.
> 지금은 편집기가 모르는 타입을 만나면 배너를 띄우고 **저장 버튼을 잠근다**
> (`admin-form-edit.js`의 `blockUnsupported`).

### 폼 대상 판정

`Form.includes(Employee)` **한 곳**에만 있다. 구분(`target`) AND 부서(`targetDepartments`).
목록·상세 접근·대상 인원 집계·대시보드가 전부 이 메서드를 쓴다. 딴 데서 다시 구현하지 말 것.

### 삭제 정책

| 대상 | 정책 |
|---|---|
| 직원 퇴사 | soft (`active=false` + `resignDate`) |
| 직원 삭제 | 응답 0건 + 본인 아님 + 슈퍼관리자 아님 + (ADMIN이면 슈퍼관리자만) → **물리 삭제** |
| 폼 삭제 | 응답 있으면 soft(`deletedAt`), 0건이면 물리 삭제 (질문·선택지 cascade) |
| 폼 복구 | 폼 관리 → "삭제됨" 탭 → 복구 |

---

## 5. 로그인 흐름 (PIN 포함)

```
이름 + 전화번호
  ├─ EMPLOYEE ─────────────────────────→ 세션 생성 → /forms
  └─ ADMIN
       ├─ pinHash 없음 → 로그인 불가 ("관리자에게 PIN 발급을 요청해 주세요")
       └─ /login/pin (숫자 6자리)
            ├─ pinChangeRequired → /login/pin/change → 새 PIN → 세션 → /admin
            └─ 아니면                                    세션 → /admin
```

**세션(`loginUser`)은 PIN까지 끝난 뒤에만 만든다.** 그 전에는 대기 표시만 세션에 둔다
(`pendingAdminEmpNo`, `pinChangePendingEmpNo`). 인터셉터는 `loginUser`를 보므로
PIN을 통과 못 한 관리자는 `/admin/**`에 접근할 수 없다.

| PIN 규칙 | 값 |
|---|---|
| 형식 | 숫자 6자리 |
| 저장 | BCrypt (`$2a$`/`$2b$`/`$2y$` 모두 인식 — 검증 완료) |
| 잠금 | 연속 5회 실패 시 10분. 잠금 중엔 맞아도 거부 |
| 같은 값으로 변경 | 저장된 해시와 대조해 거부 (강제 변경 흐름 포함) |
| 발급·초기화 | 슈퍼 관리자만. 직원 수정 모달에서 |
| 본인 변경 | 관리자 사이드바 하단 `PIN 변경` |

**PIN 원문은 로그·예외 메시지 어디에도 남기지 않는다.** (검증으로 확인함)

`LoginUser`는 `(empNo, name)` 2개뿐인 record이고 세션에 직렬화된다.
**모양을 바꾸면 배포 시 로그인된 사용자가 전부 튕긴다.**

---

## 6. 화면과 엔드포인트

### 사용자
```
GET/POST /login              이름+전화번호
GET/POST /login/pin          관리자 PIN
GET/POST /login/pin/change   첫 로그인 강제 변경
POST     /logout
GET      /forms              홈 (신청 가능한 폼)
GET/POST /forms/{id}         폼 작성·제출
GET      /forms/{id}/complete
GET      /responses          신청내역 + 기본 배송지 관리
POST     /responses/default-address        기본 배송지 등록·수정
POST     /responses/default-address/delete
```

### 관리자 (`/admin/**` — `AdminInterceptor`가 매 요청 DB에서 role 재확인)
```
/admin                       대시보드
/admin/forms                 폼 관리 (검색·상태탭 open/closed/all/deleted)
/admin/forms/{id}/edit       폼 편집
/admin/forms/{id}/delete     삭제 (soft/hard 자동 판정)
/admin/forms/{id}/restore    복구
/admin/responses             응답 현황 (목록/통계 탭, 엑셀 다운로드)
/admin/employees             직원 관리 (정렬·필터·엑셀 업로드/다운로드)
/admin/employees/{empNo}/delete, /resign
/admin/settings              로그인 문구·문의 담당자·부서
/admin/pin                   본인 PIN 변경
```

---

## 7. 주요 기능 메모

**폼 편집** — 질문을 통째로 갈아끼운다(`replaceQuestions`). 응답이 있는 폼은
서버에서 `locked(id)`를 확인해 **마감일만** 바꾼다. 질문 카드는 JSON payload를
JS가 그려서, 최초 로드와 검증 실패 후 재렌더가 같은 코드 경로를 탄다.

**직원 엑셀** — 컬럼 고정: 사번/이름/부서/구분/전화번호/입사일.
**파싱은 컬럼 위치로 한다**(헤더 이름을 검사하지 않음) → 예전 "직군" 헤더 파일도 그대로 올라간다.
업로드 양식은 서식 포함(헤더 #009CA6, 틀 고정, 구분 드롭다운, 날짜 서식, 맑은 고딕).
`template()`과 `export()`는 경로가 분리돼 있다.

**응답 엑셀** — 컬럼 순서는 `[고른 직원 정보] → [응답 시각·수정 여부] → [고른 질문]`.
모달에서 체크·순서(▲▼)를 정하고, **체크된 입력이 넘어가는 순서가 곧 컬럼 순서**다
(정렬값을 따로 담지 않고 DOM을 옮김). 주소는 `(35285) 대전 서구 동서대로 967 2동 1504호` 한 칸.

**전화번호** — `common/PhoneFormat.normalize()` 한 곳에서 `010-1234-5678`로 통일.
모달·엑셀 양쪽 경로에 적용. 표기가 갈리면 UNIQUE와 중복 검사가 무력해지기 때문
(실제로 같은 번호가 두 사람으로 등록되던 버그가 있었다).

**기본 배송지** — ADDRESS 질문 아래 "기본 배송지로 저장" 체크박스(기본 체크).
폼을 열 때 **저장된 응답이 없는 ADDRESS 질문만** 기본 배송지로 채우고 안내 문구를 띄운다.
관리자 화면·엑셀에는 노출하지 않는다.

---

## 8. 운영 배포

| 항목 | 값 |
|---|---|
| 호스트 | AWS Lightsail Ubuntu 24.04, 2GB |
| 서버 | `15.164.128.111` / `idis-welfare.co.kr` |
| 구성 | Nginx(443) → Spring Boot(8080, systemd `idis`) → MySQL(localhost) |
| 앱 경로 | `/opt/idis/idis.jar`, 백업 `idis.jar.bak` (한 세대만) |
| 로그 | `/var/log/idis/idis.log` (30일 보관) |
| 프로파일 | `prod`. **`ddl-auto=validate`** |

### 배포 방법

```powershell
.\deploy\deploy.ps1 -Yes          # 빌드 → 업로드 → 교체 → /login 200 대기
.\deploy\deploy.ps1 -SkipBuild    # 이미 만든 jar
```

`deploy/setup.md`에 서버 최초 구축 11단계와 재배포 절차가 있다.

### ⚠ 스키마가 바뀌면 스크립트만으로 안 된다

운영은 `validate`라 엔티티와 테이블이 다르면 **앱이 아예 안 뜬다.**
엔티티 필드 추가·삭제 / enum 값 추가 / 테이블 신설이 있으면
**`ALTER`를 먼저 넣고** 배포한다. `deploy.ps1`이 실행 시 이걸 묻는다.

---

## 9. 함정 모음 (실제로 겪은 것들)

**systemd `Requires=mysql.service` 쓰지 말 것**
자동 보안 업데이트가 MySQL을 재시작하면 앱도 함께 멈추고 **다시 안 올라온다.**
2026-09-02 새벽에 이 일로 4시간 반 502가 떴다. 지금은 `Wants=` + `Restart=always`로 고쳐 뒀다.

**`mysqldump`로 `schema.sql`을 뽑을 때 3개 옵션 필수**
`--set-gtid-purged=OFF`(SUPER 권한 필요 구문 제거) `--skip-add-drop-table`(재실행 시 운영 DB 날아감)
`sed 's/ AUTO_INCREMENT=[0-9]*//'`

**PowerShell 5.1**
- 파일에 한글이 있으면 **UTF-8 BOM**으로 저장해야 한다. BOM 없으면 ANSI로 읽어 깨진다
- `$ErrorActionPreference='Stop'`이면 네이티브 명령이 stderr 한 줄만 써도 끊긴다 → `Invoke-Native`로 감쌈
- 문자열을 `ssh`로 파이프하면 BOM이 붙는다 → base64로 실어 보낸다
- here-string이 CRLF면 원격 bash가 죽는다 → 보내기 전 `\r` 제거
- `&&`, 삼항 연산자, `??` 없음

**Thymeleaf**
- `th:attr`은 값이 빈 문자열이면 속성을 아예 안 그린다
- `th:classappend`가 붙으면 `class="row"`가 `class="row is-admin"`이 되어 문자열 매칭이 깨진다

**CSS**
- `flex: 1`은 `border-box`에서 테두리가 `flex-basis`에 포함돼 폭이 어긋난다. 정확히 반씩 나누려면 grid `1fr 1fr`
- 전역 셀렉터(`html`, `body.app`)를 건드리면 관리자 화면까지 영향받는다. `html:has(body.app)`처럼 좁힐 것

**Chrome 헤드리스** — 이 환경에서 뷰포트가 500px 미만으로 안 내려간다.
진짜 모바일 폭으로 재려면 로컬 HTTP로 띄운 iframe 안에서 측정해야 한다.

---

## 10. 로컬 개발

```powershell
.\gradlew bootRun        # http://localhost:8080
```

`src/main/resources/application-local.properties`가 필요하다 (**git에 없음**).
`application-local.properties.example`을 복사해 DB 접속 정보를 채운다.
이 파일은 `bootJar`에서 제외되므로 배포 jar에는 안 들어간다.

### 시드 (순서대로)

```
seed/schema.sql          스키마 (운영도 이걸로)
seed/employee.sql        홍길동(직원) + 김관리(슈퍼관리자)
seed/employee-demo.sql   직원 24명 + 부서 7개
seed/sample-form.sql, form-admin-demo.sql, response-demo.sql, site-setting.sql
```

**로컬 관리자 로그인**: 김관리 / `010-9876-5432` / **PIN `000000`**

> `seed/employee.sql`은 **로컬 전용**이다. `super_admin=1` 가짜 관리자를 만들기 때문에
> 운영에 넣으면 안 된다. 운영은 `schema.sql` + `prod-init.sql` 둘뿐이다.

---

## 11. 지금 열려 있는 것

**복직 기능 없음** — 잘못 퇴사 처리하면 화면에서 되돌릴 방법이 없다.
`Employee.reinstate()`가 있었는데 호출처가 없어 죽은 코드로 지웠다.
지금은 DB에서 `active=1, resign_date=NULL`로 직접 고쳐야 한다.

**삭제된 폼의 응답 열람** — 삭제됨 탭에서는 복구만 된다.
응답을 보려면 먼저 복구해야 한다. 관리자 쪽 응답 조회 4곳이 `deletedAt is null`로 거르기 때문.

**퇴사자 삭제 허용은 의도된 상태** — 응답이 없으면 퇴사자도 지워진다.
지우면 `이번 달 퇴사` 통계에서 소급해 빠진다. 논의 끝에 현행 유지로 결정.

**정리 후보(미실행)** — `Attachment` 계열은 이미 지웠고,
`startAt`(예약 게시)과 `FormStatus.DRAFT`(임시저장)는 UI만 없는 미구현 기능으로 **남겨 두기로** 결정.
`icon-check.svg` / `icon-chevron-down.svg`는 미참조지만 git 미추적이라 보류.

**CSS 중복 통합** — `.empty-state`↔`.center-block`, `.dept-badge`↔`.dept-chip`.
둘 다 실사용이라 삭제가 아니라 통합 대상. 배포 후로 미뤘다.

---

## 12. 더 볼 곳

| 문서 | 내용 |
|---|---|
| `docs/spec.md` | 엔티티·대상 조건·질문 타입·검증 규칙·관리자 규칙 상세 |
| `deploy/setup.md` | 서버 구축 11단계, 재배포, PIN 복구, 502 대응 |
| `docs/design-prompt.md` | Figma 시안 원본 프롬프트 |
