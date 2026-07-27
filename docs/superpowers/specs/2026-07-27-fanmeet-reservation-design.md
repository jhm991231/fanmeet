# 팬미팅/시참 슬롯 예약 서비스 — v1 설계 문서

- 작성일: 2026-07-27
- 상태: 사용자 승인 대기
- 프로젝트 가칭: fanmeet (이름 미정)

## 1. 배경과 목적

project_amoroso에서 프론트엔드를 담당하며 백엔드가 사용한 OAuth2, JWT, PG 결제, SMS 발송을
구경만 했던 것을, 이번에는 직접 구현하며 학습한다. 학습이 최우선이되 완성 시 포트폴리오로
쓸 수 있는 수준을 목표로 한다.

**서비스 개념**: 스트리머(호스트)가 팬미팅·시참 이벤트를 개설하면, 팬이 소셜 로그인 후
원하는 시간대(슬롯)를 골라 예약금을 결제하고, 확정 문자를 받는 예약 서비스.

**v2 진화 계획**: v1 완성 후 "선착순 오픈" 이벤트를 추가해 대기열·분산 락·부하테스트 등
동시성 주제로 확장한다. v1의 단순한 정원 검증 방식이 고부하에서 무너지는 지점이
v2의 출발점이 된다.

## 2. v1 범위

| 기능 | 내용 | 학습 목표 |
|---|---|---|
| 소셜 로그인 | 카카오 + 구글 | OAuth2 |
| 인증/인가 | Access/Refresh 토큰, HOST 역할 기반 이벤트 개설 권한 | JWT |
| 휴대폰 인증 | 번호 등록 시 SMS 인증번호 검증 | SMS |
| 이벤트 관리 | 호스트의 이벤트·슬롯 등록/수정/마감 | 도메인 CRUD |
| 예약 + 결제 | 슬롯 선택 → PortOne 결제(테스트 모드) → 서버 검증 → 확정 | 결제 |
| 취소/환불 | 예약 취소 시 PortOne 환불 API 호출 | 결제 취소 |
| 문자 알림 | 예약 확정·취소 시 SMS 발송 | SMS |
| API 문서 | Swagger (springdoc) | 실무 습관 |

**v1에서 의도적으로 제외 (v2 이후)**: 선착순 대기열, 분산 락, 부하테스트, 정기결제,
치지직 방송 API 연동, 계정 통합(카카오·구글 각각 가입하면 별도 계정으로 둔다).

## 3. 기술 스택

amoroso와 동일 계열로 맞춘다(학습 목적상 의도적 선택).

- **백엔드**: Java 21, Spring Boot 4.x (설계 시점엔 3.x였으나 OSS 지원 종료로 착수 시 4.1로 결정),
  Spring Security + OAuth2 Client,
  Spring Data JPA + MySQL 8, jjwt, PortOne 서버 SDK, CoolSMS(Nurigo) SDK, springdoc
- **프론트**: React (Vite), 백엔드 학습이 주목적이므로 최소한의 화면만
- **로컬 인프라**: Docker Compose로 MySQL. Redis는 v1에서 사용하지 않는다
  (Refresh 토큰은 DB 저장, 인증번호도 DB 테이블 — v2에서 Redis 도입 시 이전 후보)

**사전 준비 (외부 계정)**
1. 카카오/구글 개발자 앱 등록 (무료)
2. PortOne 계정 + 테스트 채널 (무료, 실결제 없음)
3. CoolSMS 계정 + 발신번호 등록 + 소액 충전 (건당 약 20원)

## 4. 아키텍처 개요

```
[React :3000] ──REST──▶ [Spring Boot :8080] ──▶ MySQL
                              │
                              ├──▶ PortOne API (결제 조회/취소)
                              ◀── PortOne 웹훅
                              └──▶ CoolSMS API (SMS 발송)
```

외부 연동(PortOne, CoolSMS)은 각각 `PaymentGateway`, `SmsSender` 인터페이스 뒤에 감춘다.
테스트에서 mock으로 교체하기 위함이며, 이것이 v1의 핵심 구조 원칙이다.

## 5. 도메인 모델

```
User       (id, provider, provider_id, nickname, phone, phone_verified, role,
            UNIQUE(provider, provider_id))
Event      (id, host_id→User, title, description, deposit_amount, status[OPEN/CLOSED])
TimeSlot   (id, event_id→Event, starts_at, ends_at, capacity)
Reservation(id, user_id→User, slot_id→TimeSlot, status, amount, created_at)
Payment    (id, reservation_id→Reservation, portone_payment_id, amount,
            status, refunded_at)   ← 예약 1 : 결제시도 N
PhoneVerification(id, phone, code, expires_at, verified, attempt_count)
```

**설계 결정 사항**

- **TimeSlot에 예약 카운터 컬럼을 두지 않는다.** 정원 검증은 예약 트랜잭션 안에서
  슬롯 행에 비관적 락(`@Lock(PESSIMISTIC_WRITE)`)을 걸고 활성 예약 COUNT로 수행한다.
  카운터 중복 저장은 정합성 사고의 온상이며, 이 방식의 성능 한계를 관측하는 것이
  v2 동시성 작업의 소재가 된다.
- **Payment는 예약에 N:1인 "결제 시도 로그"다.** 결제 실패·환불 후 재시도가 자연스럽게
  기록되며, "성공 결제는 예약당 최대 1건" 규칙은 서비스 로직에서 지킨다.
- **중복 예약 방지는 서비스 로직으로.** 취소 후 재예약을 허용해야 하므로 DB 유니크
  제약은 쓰지 않고, 슬롯 락과 같은 트랜잭션에서 활성 예약 존재 여부를 검사한다.
- **역할 정책**: 모든 유저는 예약 가능. HOST는 이벤트 개설 권한이 있는 부가 역할이며
  JWT 클레임 + `@PreAuthorize`로 개설 API만 제한한다.
- **금액은 정수(원화).** Reservation.amount는 예약 시점의 예약금 스냅샷으로,
  호스트가 이후 금액을 바꿔도 기존 예약의 검증 기준이 흔들리지 않게 한다.
- **Reservation.status 전이**: `PENDING_PAYMENT → CONFIRMED → CANCELLED`,
  `PENDING_PAYMENT → EXPIRED`(30분 미결제).

## 6. 인증 설계 (OAuth2 + JWT)

- Spring OAuth2 Client의 인가 코드 플로우 사용. 로그인 성공 시 SuccessHandler가
  JWT를 발급하고 프론트 콜백 URL로 리다이렉트한다.
- **Access 토큰**(30분): 응답 본문/리다이렉트로 전달, 프론트가 메모리에 보관.
- **Refresh 토큰**(14일): HttpOnly 쿠키로 전달, DB에 저장. 재발급 시 DB 대조 후
  기존 토큰 폐기(rotation). 로그아웃 = DB에서 삭제.
- JWT 클레임: userId, role. 서명은 HS256 + 환경변수 시크릿.

## 7. 휴대폰 SMS 인증

소셜 로그인은 휴대폰 번호를 주지 않으므로, 첫 예약 전 번호 등록을 요구한다.

1. 번호 입력 → 서버가 6자리 인증번호 생성, PhoneVerification 저장(만료 5분), SMS 발송
2. 사용자가 코드 입력 → 대조 성공 시 User.phone/phone_verified 갱신
3. 남용 방지: 같은 번호 재발송 60초 제한, 검증 시도 5회 초과 시 해당 코드 무효화
4. 예약 API는 phone_verified=true를 요구한다

## 8. 예약·결제 플로우

**원칙: 프론트가 보내는 결제 정보는 신뢰하지 않는다. 금액 검증은 항상 서버가 한다.**

1. 예약 신청 → 슬롯 락 + 정원/중복 검증 → Reservation(PENDING_PAYMENT) 생성,
   결제 금액·주문 식별자 응답
2. 프론트가 PortOne SDK로 결제창 호출
3. 결제 완료 콜백(paymentId) 수신 → 서버가 PortOne API로 결제 조회
4. 조회된 실결제 금액과 Reservation.amount 대조
   - 일치: Payment(PAID) 기록, Reservation → CONFIRMED, 확정 SMS 발송
   - 불일치: 즉시 PortOne 취소(환불), Payment(REFUNDED) 기록, 예약은 PENDING 유지
5. **웹훅**: 사용자가 결제 직후 이탈해도 PortOne 웹훅으로 동일 검증을 수행한다.
   콜백·웹훅이 중복 도착해도 결과가 같도록 멱등 처리한다
   (이미 CONFIRMED면 무시 — portone_payment_id 기준).
6. **만료 처리**: `@Scheduled` 스케줄러가 30분 경과한 PENDING_PAYMENT를 EXPIRED로
   전환해 자리를 반환한다.

## 9. 취소/환불

- 본인 예약 + CONFIRMED 상태만 취소 가능. 슬롯 시작 이후에는 취소 불가(v1 정책).
- PortOne 환불 API 성공 → Payment(REFUNDED), Reservation(CANCELLED), 취소 SMS.
- 환불 API 실패 → Payment(REFUND_FAILED)로 기록하고 예약은 CONFIRMED 유지,
  이후 재시도 또는 수동 처리 대상으로 남긴다.

## 10. SMS 알림

- 발송 시점: 예약 확정, 예약 취소(환불 완료), 휴대폰 인증번호.
- **SMS 실패는 핵심 트랜잭션을 막지 않는다.** 예약 확정 커밋 후 발송하며,
  실패 시 로그만 남긴다.

## 11. 에러 처리

- `@RestControllerAdvice`로 일관된 에러 응답 포맷(코드, 메시지) 통일.
- 결제 완료 후 검증 단계에서 실패(금액 불일치 등)하면 반드시 환불을 시도한다 —
  사용자 돈을 물고 있는 상태를 만들지 않는다.
- 주요 도메인 예외: 정원 초과, 중복 예약, 미인증 휴대폰, 만료된 예약에 대한 결제 등.

## 12. 테스트 전략

- 서비스 로직 단위 테스트(JUnit5 + Mockito): 정원/중복 검증, 금액 대조, 상태 전이,
  인증번호 만료·시도 제한. 외부 API는 `PaymentGateway`/`SmsSender` mock으로 대체.
- 통합 테스트는 예약 생성·취소 API 중심으로 소수만(H2). v1에서 커버리지 욕심은 내지 않는다.

## 13. v2 로드맵 (참고용, 본 설계 범위 아님)

- 선착순 오픈 이벤트: 대기열, Redis 분산 락 vs DB 락 비교, k6 부하테스트
- Refresh 토큰·인증번호 저장소를 Redis로 이전
- 치지직 API 연동(방송 상태), 정기결제 등
