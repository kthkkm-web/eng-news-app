# 마이루틴 (MyRoutine)

개인용 습관·루틴 트래커 안드로이드 앱. 기존에 쓰던 루틴 앱의 기능 구조·UX 흐름을 참고해 새로 만든다
(원본 앱의 이름·로고·일러스트·브랜드 요소는 쓰지 않음, 광고/구독/공유/칭찬 메뉴 없음).

## 기술 스택
- Kotlin 2.2, Jetpack Compose (Material 3), 단일 Activity + Navigation Compose
- MVVM + Repository, Hilt DI, Coroutines/Flow
- Room 2.8 (스키마 export → `app/schemas/`), DataStore Preferences
- Jetpack Glance 위젯, AlarmManager + WorkManager 알림 (Phase 5~6)
- AGP 8.13.2 / Gradle 8.14.3 / compileSdk·targetSdk 36 / minSdk 26
- applicationId / namespace: `app.myroutine`

## 폴더 구조 (`app/src/main/java/app/myroutine/`)
```
MyRoutineApp.kt        Application (Hilt, 기본 태그 시드)
MainActivity.kt        단일 Activity
core/                  UUID, TimeProvider, DataChanges(변경 신호 버스)
auth/                  LocalAuthRepository(로컬 사용자 1명), NoopSyncRepository
domain/model/          순수 도메인 모델 (Routine, RepeatRule, RoutineLog, Tag, Todo, User)
domain/schedule/       ScheduleResolver — 반복 규칙 해석 순수 함수 (유닛 테스트 대상)
domain/repository/     Repository 인터페이스 (Auth, Sync, Routine, Tag, Todo)
data/db/               Room 엔티티·DAO·Converters·Mappers·AppDatabase
data/repository/       Repository 구현
data/settings/         DataStore 설정 (보기 모드, 그룹 모드, 테마, 주 시작 요일, 하루 시작 시각 …)
di/                    Hilt 모듈
ui/theme/              AppColors 토큰(다크/라이트), RoutinePalettes(루틴 색상 테마)
ui/navigation/         하단 탭 4개 + NavHost
ui/…                   화면별 패키지 (Phase 2~)
widget/                Glance 위젯 (Phase 5)
notification/          알림 (Phase 6)
```

## 빌드 명령어
```bash
export ANDROID_HOME=$HOME/android-sdk        # local.properties 의 sdk.dir 로도 지정됨
./gradlew testDebugUnitTest                  # 유닛 테스트
./gradlew assembleDebug                      # → app/build/outputs/apk/debug/app-debug.apk
```
- Maven Central 이 가끔 429(요청 과다)를 돌려준다 → `--no-parallel` 로 재시도.
- GitHub Actions(`.github/workflows/build.yml`)가 push 마다 테스트 + 디버그 APK 를 빌드해
  Actions 탭의 artifact `myroutine-debug-apk` 로 올린다.
- 서명: `keystore/myroutine-dev.keystore`(비밀번호 `myroutine`)로 debug/release 모두 서명.
  어디서 빌드해도 같은 키 → 덮어쓰기 설치 시 데이터 유지. 공개 배포용 아님.

## 데이터 모델 원칙
- 모든 엔티티: `id`(UUID 문자열), `userId`, `createdAt`, `updatedAt`, `deletedAt?`(soft delete)
- 날짜 `"yyyy-MM-dd"`, 시각 `"HH:mm"` 문자열 저장. FK 없음(동기화 순서 문제 회피).
- RoutineLog 는 (routineId, date) 유니크. 기록 취소는 행 삭제가 아니라 count=0/value=null.
- 스키마 변경 시 `AppDatabase.version` 증가 + Migration 추가 (`DatabaseModule`).

## 결정 사항 (애매한 부분을 이렇게 정함)
1. 같은 날 여러 반복 규칙이 겹치면 목표치 **합산**, 시각은 합집합.
2. "목표치 설정" 토글 OFF → 그날 목표 1회. 수치형 루틴도 목표 1(값 있으면 완료).
3. "주중 아무 때나 N회": 그 주 다른 날 합계가 N 이상이면 그날은 해당 없음, 아니면 매일 해당·목표 1.
   홈 기본 그룹에서 "중장기 루틴"으로 분류.
4. 월간 규칙에서 없는 날짜(31일 등)는 그 달 말일로 취급. 연간 2/29 는 평년에 2/28.
5. 하루 달성률 = Σmin(완료,목표)/Σ목표 → 날짜 링·히트맵 농도.
6. 하루 시작 시각(0~6시) 이전 시각은 전날로 본다(`ScheduleResolver.logicalDate`).
7. 카드형 하단 3칸 = 선택 날짜 이전, 루틴이 해당했던 최근 3일의 달성 상태.
8. 루틴 색상: 5색 묶음 "색상 테마" 12종 중 선택 → 루틴 색 1개. 저장 키 `"themeId:index"`. 잠금 테마 없음.
9. **스와이프**: 홈 루틴 항목을 왼쪽으로 밀면 그날 목표치까지 완료 처리, 오른쪽으로 밀면 그날 기록 취소(0으로).
   실수 대비 스낵바 "되돌리기" 제공.
10. 강조색 민트(#3DDC97), 기본 다크 테마.

## 진행 상황
- [x] Phase 1: 프로젝트 세팅, Room 엔티티/DAO, Auth·Sync 스텁, ScheduleResolver + 유닛 테스트 13개
- [ ] Phase 2: 홈(주간 스트립, +1 기록, 카드형, 스와이프) + 루틴 추가·수정
- [ ] Phase 3: 보기 모드 3종, 그룹 모드, 필터, 드래그 정렬, 할 일
- [ ] Phase 4: 타임라인·통계
- [ ] Phase 5: Glance 위젯 (5a 2x2 3종 / 5b 흐름·일일·주간 / 5c 그래프 2종)
- [ ] Phase 6: 알림
- [ ] Phase 7: 추천루틴, 내정보 설정, 백업/복원
- [ ] Phase 8: 마무리, 릴리스 빌드 문서

## TODO / 메모
- targetSdk 37 플랫폼이 나와 있으나 AGP 8.13 기준 36 사용. AGP 9 이전 후 올릴 것.
