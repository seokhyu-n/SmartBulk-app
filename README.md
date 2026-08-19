# SmartBulk

AI 기반 개인 맞춤 운동 추천, 실시간 자세 피드백, AI 식단 추천을 제공하는 Android 헬스케어 애플리케이션

## 프로젝트 소개

SmartBulk는 사용자의 신체 정보(나이, 키, 몸무게)와 운동 목표, 그리고 날짜별로 직접 설정한 운동 부위 계획을 바탕으로 맞춤 운동 루틴을 추천합니다. 카메라로 실시간 자세를 인식해 운동 중 피드백을 제공하고, 그날 운동 부위에 맞춰 Claude(Anthropic)가 생성한 식단을 추천합니다.

## 주요 기능

### 사용자 관리
- 회원가입 / 로그인 (자동 로그인 지원)
- Firebase Realtime Database 기반 프로필 저장
- 운동 목표 설정

### 날짜별 맞춤 운동 계획
- 통계 화면에서 이번 주 날짜별로 운동 부위(가슴·삼두 / 등·이두 / 하체 / 어깨 / 휴식)를 직접 설정
- 메인 화면의 "오늘의 추천 루틴"과 "운동 시작하기"가 항상 동일한 기준으로 같은 루틴을 보여줌
- 모든 루틴은 [준비 스트레칭 → 부위별 운동 → 유산소 → 마무리 스트레칭] 순서로 자동 구성
- "오늘의 운동" 화면에서 운동마다 완료 체크박스를 제공하고, 전부 체크해야 "운동 완료" 버튼이 활성화되는 체크리스트 방식
- 달력에서 날짜를 탭하면 그날의 계획을 미리보기로 확인 가능, 완료한 날짜는 달력에 표시

### 카메라 기반 자세 피드백
- TensorFlow Lite MoveNet으로 온디바이스 포즈 추정 (회전 보정, 정사각형 크롭, 프레임별 관절 좌표 추출)
- 운동마다 독립된 판정 로직(`ExerciseAnalyzer`)을 두어, 운동 종류에 맞는 자세 규칙과 반복 횟수 카운팅을 적용
- 스쿼트·런지·사이드 레터럴 레이즈·밀리터리 프레스·해머 컬·케이블 푸쉬다운·덤벨 트라이셉스 익스텐션·바벨 로우·티바 로우·풀업 등 10종 운동 지원
- 메인 화면 "배우고 싶은 운동"에서 지원 운동을 골라 3회까지만 카운트하는 연습 모드로 자세를 미리 익힐 수 있음

### AI 식단 추천
- 사용자 프로필과 그날 운동 부위를 바탕으로 Claude(Anthropic API)가 아침·점심·저녁·간식과 한줄 팁을 생성
- Anthropic API 키는 클라이언트에 두지 않고 Firebase Cloud Functions에서만 사용해, 앱 바이너리에 키가 노출되지 않도록 설계
- 같은 날짜의 추천은 Realtime Database에 캐싱해 하루 한 번만 실제 API를 호출

### 운동 통계
- 이번 달 총 운동 일수, 이번 주 운동 횟수, 연속 운동 일수 집계
- 달력에 요일 헤더와 완료 여부를 함께 표시

## 기술 스택

### Mobile
- Kotlin, Android (ViewBinding / DataBinding)
- CameraX
- Material 3 기반 다크 테마 UI

### Backend / Cloud
- Firebase Authentication
- Firebase Realtime Database
- Firebase Cloud Functions (Node.js / TypeScript) — Anthropic API 프록시 및 결과 캐싱

### AI / Computer Vision
- TensorFlow Lite MoveNet — 온디바이스 실시간 포즈 추정
- Anthropic Claude API — 서버 사이드 식단 추천 생성 (구조화된 JSON 출력)

## 프로젝트 구조

```text
app/
 ├── MainActivity                 로그인 후 첫 화면 — 프로필, 오늘의 루틴, 식단 추천, 배우고 싶은 운동
 ├── StatisticsActivity           날짜별 운동 부위 설정, 달력, 운동 통계
 ├── WorkoutDetailActivity        오늘의 운동 목록 + 완료 체크리스트
 ├── ExerciseDetailActivity       운동 상세 설명 + 자세 피드백 진입점
 ├── PoseFeedbackActivity         카메라 프리뷰 + 실시간 자세 판정 화면
 ├── MoveNetPoseEstimator         카메라 프레임 → 관절 좌표 추론 (모델/카메라 배관 담당)
 ├── ExerciseAnalyzer 구현체들     운동별 자세 판정 로직 (SquatAnalyzer, LungeAnalyzer 등)
 └── util/
      ├── WorkoutRecommender      목표·부위·날짜에 맞는 루틴 생성
      └── DietRecommender         Cloud Function 호출해 식단 추천 가져오기

functions/
 └── src/index.ts                 getDietRecommendation — Claude API 호출 + Realtime DB 캐싱
```

## 기술적 도전 과제

- **자세 인식 파이프라인**: 카메라 회전 보정, NV21 프레임 변환의 stride 처리, TFLite 입력 버퍼 `rewind()` 누락, MoveNet 입력 정규화 범위(0~255 vs 0~1) 등 코드 리뷰만으로는 드러나지 않고 실제 기기에서 카메라를 켜봐야만 확인되는 버그들을 하나씩 찾아 고쳐가며 클린 아키텍처(추론 담당 클래스 / 운동별 판정 클래스 분리)로 재작성했습니다.
- **운동별 자세 판정 일반화**: 사람마다 촬영 각도·거리·신체 비율이 달라 관절 좌표 기반 임계값을 하나로 일반화하기 어려웠습니다. 운동마다 독립된 `ExerciseAnalyzer`를 두어 히스테리시스 기반 상태 머신으로 반복 횟수를 판정하는 구조로 해결했습니다.
- **API 키 보안**: 모바일 앱에 외부 API 키를 직접 넣으면 APK 디컴파일로 노출될 수 있어, Firebase Cloud Functions를 프록시로 두고 클라이언트는 인증된 사용자만 호출 가능한 Callable Function만 알도록 설계했습니다.

## 향후 개선 방향

- 나머지 운동들의 자세 판정 임계값을 실제 기기 테스트로 튜닝
- 벤치프레스 / 레그프레스 / 시티드 체스트프레스 등 거치대가 필요하거나 기구에 가려지는 운동의 자세 분석 지원
- 브랜드 컬러 추가 조정

## 실행 화면

<table>
  <tr>
    <td align="center"><img src="screenshots/login.png" width="220"><br>로그인 화면</td>
    <td align="center"><img src="screenshots/signup.png" width="220"><br>회원가입 화면</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/mypage.png" width="220"><br>마이페이지</td>
    <td align="center"><img src="screenshots/main.png" width="220"><br>메인 화면</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/learn_exercises.png" width="220"><br>배우고 싶은 운동 목록</td>
    <td align="center"><img src="screenshots/statistics.png" width="220"><br>통계/달력 화면</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/statistics_preview.png" width="220"><br>통계 - 날짜 미리보기 다이얼로그</td>
    <td align="center"><img src="screenshots/workout_checklist.png" width="220"><br>오늘의 운동 - 체크박스</td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/exercise_detail.png" width="220"><br>운동 상세 화면 (스쿼트)</td>
    <td align="center"><img src="screenshots/pose_feedback.png" width="220"><br>스쿼트 인식 자세 피드백</td>
  </tr>
</table>

## 개발 기간

2025 ~ 진행중 (정확한 기간은 직접 채워주세요)

## 느낀 점

모바일 앱 개발과 Firebase 기반 사용자 관리, 온디바이스 컴퓨터 비전(TensorFlow Lite), 서버를 경유한 외부 LLM API 연동까지 함께 구현하며, 실시간 피드백 시스템 설계와 컴퓨터 비전 적용 과정의 어려움, 그리고 클라이언트에 민감한 키를 두지 않는 아키텍처 설계 경험을 얻었습니다.
