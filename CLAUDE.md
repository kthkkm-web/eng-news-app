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

## 릴리스 APK 빌드·설치
```bash
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk (R8 축소, 약 3MB)
```
- 지금은 release 도 저장소의 개발용 키(`keystore/myroutine-dev.keystore`)로 서명한다 → debug/release/CI 빌드끼리 덮어쓰기 설치 가능.
- 폰 설치: APK 를 폰으로 옮겨 열기 → "출처를 알 수 없는 앱 설치" 허용. 또는 `adb install -r app-release.apk`.
- GitHub Actions: push 마다 artifact `myroutine-apk`(release+debug) 생성 → Actions 탭에서 받기.
- 업데이트할 때는 `app/build.gradle.kts` 의 `versionCode` 를 올린다(같거나 낮으면 설치가 거부될 수 있음).
- 나중에 Play 스토어 등 공개 배포 시: 새 업로드 키를 만들어 `signingConfigs` 에 `release` 를 추가하고,
  비밀번호는 `~/.gradle/gradle.properties` 나 CI secret 으로 주입(저장소에 커밋 금지). 키를 바꾸면 기존 설치본 위에 덮어쓸 수 없으니
  그 전에 앱의 '데이터 백업/복원'으로 백업할 것.
- R8 keep 규칙: `app/proguard-rules.pro` (Glance ActionCallback, Worker, kotlinx.serialization).

## 화면 스크린샷 (시각 확인)
에뮬레이터 없이 Robolectric + Roborazzi 로 실제 앱(Hilt+Room)을 렌더링한다.
```bash
./gradlew testDebugUnitTest -Pscreenshots --tests '*ScreenshotTest*'   # → app/build/screenshots/*.png
```
`-Pscreenshots` 없이 돌리면 스크린샷 테스트는 건너뛴다. Robolectric 런타임 jar 는 Gradle 이 받아 오프라인 모드로 쓴다.

## 위젯 구조 (`widget/`)
- `DataWidget<T>`: Preferences 상태의 `VERSION`/`TARGET` 이 바뀌면 `load()` 를 다시 실행 → 재구성.
- `WidgetUpdater.updateAll()`: 모든 위젯 VERSION 갱신. 앱의 `DataChanges` 신호(400ms 디바운스)와
  `DayRolloverWorker`(하루 시작 시각마다), 위젯 액션 후에 호출.
- 그래프·원형 링·알약은 `ChartRenderer`/`WidgetBitmaps` 로 비트맵을 그려 `Image` 로 넣는다(밀도 최대 2.5배).
- 설정: `WidgetConfigActivity` (그래프 → 수치 루틴, 달성 흐름 → 태그/루틴, 오늘의 루틴·주간 → 태그).
  `configuration_optional` 이라 설정 없이도 추가되고 나중에 길게 눌러 재설정 가능(Android 12+).
- 위젯 스크린샷: `./gradlew testDebugUnitTest -Pscreenshots --tests '*WidgetScreenshotTest*'`
  (주의: LazyColumn 목록 항목은 Robolectric 에서 렌더링되지 않음 → 실기기 확인 필요)

## 알림 구조 (`notification/`)
- `ReminderScheduler.plan()`(순수 함수, 테스트됨) → 오늘·내일 알림 목록 → `rescheduleAll()` 이 AlarmManager 에 등록.
  정확한 알람 권한이 없으면 `setAndAllowWhileIdle`(수 분 지연 가능)로 대체.
- 재예약 시점: 데이터 변경(디바운스), 설정 변경, 알림 발화 직후, `DayRolloverWorker`(매일), `ReminderRefreshWorker`(6시간),
  `RescheduleReceiver`(BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED, 정확한 알람 권한 변경).
- 발화 시(`ReminderReceiver.handle`) 그날 이미 목표를 채웠으면 보내지 않는다. 액션 "완료 +1" → `ReminderActionReceiver.handle`.
- 권한: 알림을 켠 규칙을 저장할 때 POST_NOTIFICATIONS 요청, 정확한 알람이 꺼져 있으면 설정 이동 안내.
- WorkManager 는 `Configuration.Provider` 로 초기화(App Startup 초기화 제거) — Robolectric 테스트와 동일 동작.

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
11. 2열 그리드 보기에서는 드래그 정렬 미지원(다른 보기에서 정렬). 스와이프는 지원.
12. 탭으로는 목표치를 넘겨 기록하지 않는다(완료 후 탭 → 안내 스낵바). 미래 날짜는 기록 불가.
13. 태그는 루틴당 여러 개 가능. 태그별 그룹에서는 해당 태그마다 표시된다.
15. 연속 달성(streak): 해당일만 센다. 미해당일은 끊지 않고, 오늘이 아직 미완료면 어제까지 기준.
16. 타임라인 상단 전체 잔디는 기간 전환과 무관하게 최근 약 1년(폭에 맞춰 칸 수 자동). '종료·보관' 칩 = 보관됐거나 종료일이 지난 루틴.
17. 달성 흐름 위젯: '달성한 날' = 대상(태그/루틴/전체)의 그날 해당 루틴을 모두 완료한 날.
18. 위젯 행 탭은 목표치를 넘기지 않는다. 수치형 루틴 행은 앱을 연다(값 입력 필요).
19. 하루 시작 시각 이전의 알림 시각(예: 시작 4시, 알림 1시)은 달력상 다음 날 그 시각에 울리고 기록은 논리 날짜로 들어간다.
20. 기본 태그는 고정 ID(`default-tag-N-userId`)로 시드 → 중복 생성 방지. 사용자가 지운 기본 태그는 되살리지 않는다.
21. 복원은 '전체 교체'(병합 아님). 설정값(DataStore)은 백업에 포함하지 않는다.
22. 추천 템플릿의 태그는 같은 이름의 기존 태그에 연결(없으면 태그 없음).
14. 할 일 정렬: 미완료 → 시간 있는 것(시간순) → 시간 없는 것.

## 진행 상황
- [x] Phase 1: 프로젝트 세팅, Room 엔티티/DAO, Auth·Sync 스텁, ScheduleResolver + 유닛 테스트 13개
- [x] Phase 2: 홈(주간 스트립, +1 기록, 카드형, 스와이프) + 루틴 추가·수정 (색상 테마 시트, 이모지 시트)
- [x] Phase 3: 보기 모드 3종, 그룹 모드, 필터, 드래그 정렬, 할 일 (Phase 2와 함께 구현)
- [x] Phase 4: 타임라인(1W/1M/1Y, 전체 잔디, 태그별 카드) · 루틴 상세(streak, 달성률, 그래프, 월 달력)
- [x] Phase 5: Glance 위젯 8종 (오늘의 루틴·할 일·월간 / 그래프 2종 / 달성 흐름·일일 달성량·주간 타임라인), 설정 Activity, 할 일 빠른 추가, 딥링크, 하루 시작 시각 갱신 Worker
- [x] Phase 6: 알림 (정확한 알람 + 미리 알림, '완료 +1' 액션, 달성 시 생략, 재부팅/시간 변경/6시간 안전망 재예약)
- [x] Phase 7: 추천루틴(16개 카테고리, 89개 템플릿 JSON), 내정보·설정 화면, 태그 관리, 보관함, 백업/복원, 위젯 가이드(실데이터 미리보기)
- [x] Phase 8: 빈 상태·에러 처리 점검, R8 keep 규칙, 릴리스 빌드 문서, 실기기 체크리스트

## 실기기 체크리스트 (삼성 갤럭시 / One UI)
1. 설치 후 첫 실행: 홈 빈 상태 → 추천루틴에서 템플릿 추가 → 홈에 표시되는지
2. 홈: 탭 +1, 왼쪽 스와이프 완료 / 오른쪽 스와이프 취소 + 스낵바 '되돌리기', 꾹 눌러 순서 변경, 보기 모드 4종 전환, 그룹 3종
3. 주간 스트립 좌우 스와이프, 월 달력에서 과거 날짜 선택 후 기록 수정
4. 루틴 추가: 색상 테마 시트, 이모지 시트, 반복 주기 2개(평일/주말), 시간·알림·미리 알림, 수치형(kg) 루틴
5. 알림: 1~2분 뒤 시각으로 알림 루틴 만들기 → 알림 수신 → '완료 +1' → 앱/위젯 반영. 목표 달성 후에는 알림이 오지 않는지
6. 재부팅 후에도 알림이 오는지 (BOOT_COMPLETED 재등록)
7. 위젯 8종 추가: 목록 위젯(오늘의 루틴·할 일·주간 타임라인)의 행 표시·스크롤·탭 동작, 그래프 위젯 크기 조절 시 다시 그려지는지
8. 위젯 설정: 길게 눌러 '설정'(재구성) → 태그/루틴 변경 반영
9. 하루 시작 시각 4시로 바꾸고 새벽 기록이 전날로 들어가는지, 자정/하루 시작 시각 이후 위젯 날짜가 넘어가는지
10. 백업 파일 만들기 → 앱 데이터 삭제 → 복원
11. **배터리 최적화 점검(One UI)**: 내정보 > 알림 설정에서 세 항목 모두 '허용됨/적용됨' 인지.
    설정 > 배터리 > 백그라운드 사용 한도 > '사용하지 않는 앱을 절전 상태로 전환' 끄기, '절전 예외 앱'에 마이루틴 추가,
    설정 > 애플리케이션 > 마이루틴 > 배터리 > '제한 없음'. 하루 이상 앱을 안 열어도 알림·위젯이 갱신되는지 확인.

## TODO / 메모
- 실기기 확인 전: Glance LazyColumn 목록 항목 렌더링(Robolectric 에서는 비어 보임), 위젯 그래프 크기(런처별 셀 크기 차이).
- `hiltViewModel` deprecation 경고: androidx.hilt 의 새 패키지(`hilt-lifecycle-viewmodel-compose`)로 이전 가능.
- 위젯 설정 화면은 Android 12 미만에서 '재구성'이 없어 위젯을 다시 추가해야 설정을 바꿀 수 있다.
- 로그인/서버 동기화: `AuthRepository`, `SyncRepository` 구현 교체 + 로컬 userId 이관.
- targetSdk 37 플랫폼이 나와 있으나 AGP 8.13 기준 36 사용. AGP 9 이전 후 올릴 것.
