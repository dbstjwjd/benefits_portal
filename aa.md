좋아. Figma AI / Make 계열에서 바로 붙여넣어도 되도록, 단순한 디자인 설명이 아니라 서비스 구조 + 화면 구성 + UX 원칙 + 반응형 + 관리자 기능 + 브랜드 가이드까지 한 번에 이해할 수 있는 형태로 작성해볼게.

IDIS 임직원 복리후생 신청 폼 플랫폼 디자인 프롬프트
1. 프로젝트 개요

IDIS 임직원이 회사에서 제공하는 복리후생 및 기념품을 간편하게 신청하고 정보를 입력할 수 있는 Google Forms와 유사한 웹 기반 폼 서비스를 디자인한다.

단순한 설문조사 서비스가 아니라, 회사의 실제 복리후생 업무에 사용할 수 있도록 다음과 같은 기능을 중심으로 설계한다.

명절 선물 선택 및 배송지/수령인 정보 입력
생일 케이크 선택
창립기념품 선택
임직원 기본정보 입력
주소 입력
날짜 선택
객관식 선택
복수 선택
주관식/서술형 입력
이미지가 포함된 선택지
파일/이미지 업로드
개인정보 입력 및 확인
신청 완료 및 수정
관리자용 응답 관리
응답률 및 현황 대시보드
사용자/임직원 정보 관리
폼 생성 및 편집
폼 배포 및 상태 관리

전체적인 서비스는 Google Forms의 익숙하고 쉬운 사용성을 참고하되, IDIS만의 브랜드 아이덴티티와 복리후생 서비스에 맞는 전문적인 UI로 재해석한다.

2. 핵심 디자인 방향

전체 서비스의 키워드:

Simple / Professional / Friendly / Reliable / Easy

기업용 서비스이지만 지나치게 딱딱한 관리자 시스템처럼 보이지 않도록 한다.

특히 임직원이 모바일에서 빠르게 신청하는 상황을 최우선으로 고려한다.

디자인은 다음 원칙을 반드시 지킨다.

UX 원칙
화면당 요구하는 결정의 수를 줄인다.
한 화면에서 너무 많은 입력이나 선택을 요구하지 않는다.
복잡한 신청은 단계별로 나눈다.
한 단계에는 명확한 하나의 목적이 있도록 한다.
익숙한 내비게이션을 사용한다.
지나치게 독창적인 navigation을 만들지 않는다.
일반적인 웹 서비스와 관리자 페이지의 관습적인 UI를 따른다.
모바일에서는 Bottom navigation 또는 직관적인 메뉴 구조를 사용한다.
즉각적인 피드백을 제공한다.
선택/입력 즉시 상태가 변화한다.
저장 중, 저장 완료, 제출 완료 상태를 명확하게 보여준다.
서버 처리나 데이터 저장에 시간이 걸릴 경우 로딩 상태와 진행 상황을 표시한다.
실수하면 쉽게 되돌릴 수 있어야 한다.
이전 단계로 돌아가기
입력 내용 수정
선택 변경
제출 전 전체 내용 확인
삭제 전 확인 모달
관리자 작업의 경우 Undo 또는 취소 기능 제공
시각적 위계를 명확하게 한다.
페이지 제목
섹션 제목
설명
입력 요소
주요 CTA
순서로 명확한 hierarchy를 구성한다.
모바일 우선으로 설계하되 PC에서도 완성도 있게 보이도록 한다.
3. 브랜드 컬러

첫 번째 첨부 이미지의 IDIS 브랜드 컬러를 기준으로 전체 UI를 디자인한다.

Main Color

IDIS GREEN / Cyan

Primary: #0199B0
RGB: 0, 156, 166 계열
핵심 CTA
선택 상태
링크
진행 상태
브랜드 포인트
주요 아이콘
강조 요소

Primary color를 서비스 전체에 과도하게 사용하지 말고, 화이트/그레이 기반 UI에 포인트 컬러로 사용한다.

기본 컬러
White: #FFFFFF
Black: #000000
Cool Gray 80: 약 #58595B
Cool Gray 40: 약 #A7A9AC
Light Gray: #F5F6F7
Border Gray: #E5E7E9
보조 컬러

첫 번째 이미지의 보조색을 참고한다.

Orange / Yellow
Light Blue Gray
Navy
Brown
Purple
Green

보조색은 카드, 카테고리, 상태, 일러스트 등에 제한적으로 사용한다.

전체적으로 IDIS Green + White + Neutral Gray가 가장 많이 보이도록 한다.

4. 로고 및 캐릭터

두 번째 첨부 이미지의 IDIS 로고를 공식 로고로 사용한다.

로고의 형태, 비율, 색상을 임의로 변경하지 않는다.

로고는 다음 위치에 활용한다.

로그인 화면
사용자 폼 Header
관리자 페이지 Header
완료 화면
Empty State
서비스 소개 영역

세 번째와 네 번째 첨부 이미지에 있는 IDIS 로봇 캐릭터를 필요할 때만 활용한다.

캐릭터를 모든 화면에 반복적으로 사용하지 않는다.

다음과 같은 상황에서 선택적으로 사용한다.

로그인
신청 완료
Empty State
오류 화면
도움말
데이터가 없는 관리자 화면
친근함이 필요한 onboarding 화면

기업용 서비스의 신뢰감은 유지하면서 캐릭터를 통해 IDIS만의 브랜드 개성을 전달한다.

5. 전체 서비스 구조
사용자 영역
1. 로그인 / 인증
IDIS 로고
서비스 이름
사내 계정 로그인
사번 / 이메일 입력
인증 또는 로그인
로그인 상태 유지
개인정보 관련 안내

깔끔한 중앙 정렬 카드 형태.

모바일에서는 화면 전체를 활용한다.

2. 복리후생 폼 목록

사용자가 접근할 수 있는 신청 목록을 보여준다.

예시:

2026년 추석 선물 신청
2026년 설 명절 선물 신청
2026년 생일 케이크 신청
IDIS 창립기념품 신청
기타 복리후생 신청

각 카드에는:

제목
간단한 설명
신청 기간
진행 상태
제출 여부
CTA

를 표시한다.

상태 예시:

신청 가능
신청 완료
신청 기간 종료
임시 저장
6. 사용자 폼 화면

Google Forms처럼 단순하고 직관적인 구조를 사용하되, IDIS 브랜드 UI로 디자인한다.

Desktop

화면 중앙에 적절한 max-width의 Form Container를 배치한다.

예:

상단 IDIS Header
Form Title Card
질문 Card
Progress 영역
하단 Navigation

배경은 아주 연한 Gray 또는 White를 사용한다.

Form Card는 과도한 그림자보다 subtle border와 radius를 사용한다.

Mobile

모바일에서는 반드시 한 손으로 조작하기 쉬운 구조로 만든다.

화면 좌우 여백 약 16~20px
충분한 터치 영역
최소 44px 이상의 버튼 영역
큰 입력창
명확한 label
키보드가 올라와도 CTA를 사용할 수 있도록 설계
긴 폼은 단계별 화면으로 분리

모바일에서는 한 화면에 너무 많은 질문을 배치하지 않는다.

7. 지원해야 하는 Form Field

다양한 입력 타입을 지원하는 Form Builder를 설계한다.

기본 입력
Short Answer
Long Answer
Number
Email
Phone Number
선택형
Single Choice
Multiple Choice
Dropdown
Image Choice
Ranking
날짜/시간
Date
Time
Date + Time
주소

주소 검색 및 상세주소 입력이 가능한 형태.

예:

우편번호
주소 검색
기본 주소
상세 주소

배송지 입력은 일반 텍스트 입력보다 주소 입력 UX를 최적화한다.

파일
이미지 업로드
파일 업로드

업로드 전/중/완료/실패 상태를 모두 디자인한다.

기타
안내 문구
Divider
Section
개인정보 동의
필수/선택 질문
8. 이미지 선택 UI

복리후생 서비스에서 매우 중요한 기능이다.

예를 들어 명절 선물 선택 시:

[ 선물 A 이미지 ]
한우 선물세트
간단한 설명
○ 선택

[ 선물 B 이미지 ]
과일 선물세트
간단한 설명
○ 선택

처럼 구성한다.

이미지를 단순히 나열하지 말고 Card Selection UI를 사용한다.

선택된 카드는:

IDIS Green Border
Light Cyan Background
Check Icon

등으로 명확하게 선택 상태를 보여준다.

모바일에서는 1열 또는 2열 Grid를 사용한다.

9. 복리후생 신청 예시 Flow
명절 선물

Step 1
선물 선택

Step 2
수령인 정보

Step 3
배송지 입력

Step 4
연락처 입력

Step 5
최종 확인

Step 6
신청 완료

각 단계에는 최소한의 정보만 요구한다.

Progress indicator를 상단에 표시한다.

예:

1 선물 선택 → 2 수령 정보 → 3 배송지 → 4 확인

현재 단계는 IDIS Green으로 강조한다.

10. 신청 완료 화면

사용자가 신청을 완료했을 때 명확한 성공 상태를 보여준다.

예:

신청이 완료되었습니다.

2026년 추석 선물 신청이 정상적으로 접수되었습니다.

신청번호
신청일시
선택한 상품
수령인
배송지 일부

를 보여준다.

개인정보는 필요한 범위에서만 노출한다.

CTA:

신청 내용 확인

홈으로 이동

필요한 경우 캐릭터를 활용해 친근한 완료 화면을 구성한다.

11. 관리자 페이지

관리자 UI는 사용자 화면과 구분되는 Professional Admin Dashboard 형태로 디자인한다.

Desktop을 중심으로 설계하고 Tablet 및 Mobile에서도 기본적인 관리가 가능하도록 Responsive하게 구성한다.

12. 관리자 Navigation

관습적인 Sidebar Navigation을 사용한다.

왼쪽 Sidebar:

Dashboard
Forms
Responses
Employees
Benefits
Statistics
Settings

하단:

관리자 프로필
로그아웃

모바일에서는 Sidebar를 Drawer 또는 Bottom Navigation으로 변환한다.

13. 관리자 Dashboard

관리자가 접속했을 때 가장 중요한 데이터를 한눈에 확인할 수 있도록 한다.

상단:

Dashboard

2026년 복리후생 신청 현황

KPI Card:

전체 대상자
응답 완료
미응답
응답률
진행 중인 폼
마감 임박 폼

예:

전체 대상자
1,248명

응답 완료
982명

미응답
266명

응답률
78.7%

응답률은 IDIS Green을 중심으로 시각화한다.

14. Dashboard Chart

다음 차트를 활용한다.

응답률

Donut Chart 또는 Progress Chart

일별 응답 현황

Line Chart

복리후생 선택 현황

Bar Chart

예:

한우 세트 42%
과일 세트 31%
건강식품 18%
기타 9%

부서별 응답률

Horizontal Bar Chart

단, 그래프를 과도하게 많이 배치하지 않는다.

중요한 정보 → 상세 정보 순으로 hierarchy를 유지한다.

15. 관리자 Form 관리

Forms 화면에서는 생성된 폼을 관리한다.

Table/Card 형태:

Form Name
Type
기간
대상자
응답률
Status
Last Updated
Action

Status:

Draft
Active
Closed
Scheduled

Action:

Edit
Preview
Responses
Duplicate
Close
16. Form Builder

관리자가 Google Forms처럼 직접 폼을 만들 수 있도록 한다.

화면 구성:

Left

질문/필드 추가

Text
Long Text
Single Choice
Multiple Choice
Dropdown
Date
Address
Image Choice
File Upload
Section
Agreement
Center

실제 폼 Preview

Right

선택한 질문의 상세 설정

예:

Question
"원하시는 명절 선물을 선택해주세요."

설정:

Required
Description
Image
Choice
Validation
Default Value

관리자가 복잡한 설정을 한 번에 보지 않도록 선택한 요소에 대한 설정만 보여준다.

17. Response 관리

Responses 화면은 관리자 업무에서 가장 중요한 화면 중 하나다.

상단:

전체 응답
완료
미완료
수정됨

검색:

이름
사번
이메일
부서

Filter:

부서
응답 상태
날짜
선택 항목

Table:

| 이름 | 사번 | 부서 | 응답 상태 | 제출일 | Action |

개별 응답을 클릭하면 Detail Drawer 또는 Detail Page를 보여준다.

18. 사용자 정보 관리

Employees 화면.

관리자는 임직원 대상자를 관리할 수 있다.

정보:

이름
사번
부서
직급
이메일
전화번호
복리후생 대상 여부
신청 상태

검색 및 Filter를 쉽게 사용할 수 있게 한다.

대량 관리가 필요한 경우:

CSV Upload
CSV Download
Import
Export

기능을 제공한다.

19. 관리자 모바일 UI

관리자 모바일 화면에서는 모든 기능을 데스크톱과 동일하게 보여주려고 하지 않는다.

모바일에서는 가장 중요한 기능 위주로 최적화한다.

Dashboard
응답률 확인
최근 응답
검색
응답 상세 확인
폼 상태 변경

복잡한 Form Builder는 모바일에서 간단한 편집 모드로 제공하거나 Desktop 사용을 유도한다.

20. 반응형 Breakpoint

최소 3가지 디자인을 제작한다.

Mobile

360~767px

Tablet

768~1199px

Desktop

1200px 이상

Desktop에서는 넓은 Container와 Sidebar를 사용하고,

Mobile에서는:

Sidebar → Drawer
Multi-column → Single-column
Table → Card/List
Horizontal controls → Vertical controls

형태로 자연스럽게 변경한다.

21. UI Component System

Figma에서 실제 개발이 가능한 수준의 Component System을 구축한다.

Components:

Button
Input
Textarea
Select
Checkbox
Radio
Toggle
Date Picker
Address Input
File Upload
Image Card
Form Card
Progress Bar
Stepper
Modal
Toast
Tooltip
Dropdown
Badge
Avatar
Table
Pagination
Tabs
Sidebar
Header
Bottom Navigation
Empty State
Loading State
Error State
Success State

모든 컴포넌트는 상태를 정의한다.

예:

Default
Hover
Focus
Active
Selected
Disabled
Error
Success
Loading

22. 접근성

기업 내부에서 다양한 연령대의 임직원이 사용하는 서비스이므로 접근성을 고려한다.

충분한 색상 대비
작은 글씨 최소화
명확한 Label
Placeholder에만 정보를 의존하지 않기
Keyboard Navigation
Focus State
명확한 Error Message
필수 입력 표시
입력 오류 발생 시 해당 필드로 이동
모바일 터치 영역 충분히 확보

특히 IDIS Green을 텍스트에 사용할 때 가독성을 확인하고, 필요한 경우 Dark Gray와 함께 사용한다.

23. 상태 디자인

모든 주요 기능에는 상태가 있어야 한다.

Loading

단순히 빈 화면을 보여주지 말고 Skeleton 또는 Spinner를 사용한다.

예:

"신청 정보를 불러오는 중입니다."

Success

IDIS Green을 활용한다.

Error

명확한 오류 이유와 해결 방법을 보여준다.

예:

"주소를 입력해주세요."

Empty

데이터가 없는 경우 캐릭터를 선택적으로 활용한다.

예:

"아직 등록된 신청서가 없습니다."

Saving

관리자가 폼을 수정할 때:

"저장 중..."

→ "저장 완료"

처럼 즉각적인 피드백을 제공한다.

24. 디자인 스타일

전체적으로 다음 스타일을 유지한다.

Clean
Modern
Minimal
Corporate
Friendly
Soft Rounded UI
High Readability

과도한 Glassmorphism, 과도한 Gradient, 복잡한 3D 효과는 사용하지 않는다.

카드의 Radius는 약 8~16px 범위에서 일관되게 사용한다.

그림자는 매우 약하게 사용한다.

White + Light Gray Background + IDIS Green Accent를 기본으로 한다.

25. Typography

한국어 가독성이 좋은 Sans-serif 계열을 사용한다.

추천:

Pretendard

또는 시스템 Sans-serif.

Hierarchy:

Display
H1
H2
H3
Body
Caption
Helper Text

관리자 화면에서는 데이터가 많기 때문에 본문과 숫자의 가독성을 최우선으로 한다.

26. 최종적으로 제작할 화면

다음 화면들을 실제 UI 디자인으로 제작한다.

사용자
Login
복리후생 목록
Form Intro
Single Choice
Multiple Choice
Image Choice
Text Input
Date Picker
Address Input
File Upload
개인정보 동의
Form Review
Submit Loading
Submit Success
신청 내역
신청 내용 수정
Error / Empty State
관리자
Admin Login
Dashboard
Forms List
Form Detail
Form Builder
Question Editor
Form Preview
Responses
Response Detail
Employees
Employee Detail
Statistics
Settings
27. 핵심 UX Flow

사용자:

로그인 → 복리후생 선택 → 폼 시작 → 질문 입력 → 선택/주소/날짜 입력 → 최종 확인 → 제출 → 완료

관리자:

로그인 → Dashboard → 폼 선택 → 응답 현황 확인 → 개별 응답 확인 → 사용자 정보 관리

관리자 Form 생성:

Forms → Create Form → 질문 추가 → 설정 → Preview → Publish → Responses

각 Flow는 최대한 단순하게 구성하고, 사용자가 현재 어디에 있는지 항상 알 수 있도록 한다.

28. 중요한 디자인 지시사항

단순히 예쁜 랜딩페이지를 만드는 것이 아니라 실제로 사용할 수 있는 기업용 Form SaaS 제품을 디자인한다.

특히 다음을 최우선으로 한다.

모바일에서 신청하기 편해야 한다.
PC에서 관리하기 편해야 한다.
처음 사용하는 사람도 별도 설명 없이 사용할 수 있어야 한다.
화면마다 가장 중요한 CTA가 하나 명확해야 한다.
입력 오류를 즉시 알려줘야 한다.
제출 전 내용을 확인할 수 있어야 한다.
제출 후 수정할 수 있는 구조를 고려한다.
관리자에게 응답률과 미응답자를 한눈에 보여줘야 한다.
데이터가 많아져도 관리하기 쉬워야 한다.
IDIS 브랜드가 자연스럽게 느껴져야 한다.
29. Figma 제작 방식

Figma에서 다음과 같이 구성한다.

Auto Layout 적극 활용
Responsive Layout
Component / Component Set
Variants
Design Tokens
Color Styles
Typography Styles
8px Grid System
Desktop / Tablet / Mobile Frame
Reusable Components

화면을 단순히 정적인 이미지처럼 만들지 말고 실제 서비스로 개발 가능한 UI System으로 구성한다.

최종 디자인은 IDIS Green #0199B0을 핵심 브랜드 컬러로 사용하고, 첫 번째 이미지의 컬러 팔레트와 두 번째 이미지의 IDIS 로고, 세 번째/네 번째 이미지의 로봇 캐릭터를 브랜드 자산으로 활용한다.

전체적인 인상은 **"Google Forms의 쉬운 사용성 + 기업용 복리후생 관리 시스템의 전문성 + IDIS의 친근한 브랜드 아이덴티티"**가 느껴지도록 디자인한다.