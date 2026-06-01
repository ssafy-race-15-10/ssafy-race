## 2026-06-01 — Task 2: computeSteering PD + EMA 구현

### 변경 파일
- `Bot_Java/MyCar.java`: computeSteering 시그니처를 7인수로 교체 (prevCenterError, prevSteering 추가). D항(K4*dCenter)과 EMA(alpha*raw + (1-alpha)*prevSteering) 적용. control_driving 호출부 임시 0f, 0f 전달.
- `Bot_Java/TestRunner.java`: testSteering 전체 교체 — EMA smoothing, D항 보정, 오버슈팅 억제 케이스 포함.

### 테스트 결과
전체 PASS (FAIL 없음). 커밋 SHA: 7e84c68

## 2026-06-01 — fix: straight-road oscillation (K2 + alpha)

**증상:** 직선도로에서 진동이 이전보다 심해짐 (이전 세션 steering 재설계 이후)

**근본 원인:**
- EMA alpha=0.40 → 1Hz 기준 위상지연 35°
- K4 수치미분 → 추가 지연 ~36°
- 누적 71° 위상지연으로 위상 여유 붕괴 → 폐루프 불안정

**수정:**
- K2: 0.20→0.35 (각도 감쇠 복원)
- alpha: 0.40→0.75 (EMA 위상지연 35°→9°)
- 대상: BASIC, SPEED, SSAFY / GERMANY는 원래 0.35 유지

**커밋:** 688543a
