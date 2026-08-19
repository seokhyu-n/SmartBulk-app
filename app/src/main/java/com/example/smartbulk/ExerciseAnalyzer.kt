package com.example.smartbulk

/** 피드백 문구의 톤 — 화면에서 색상을 다르게 표시하는 데 사용 */
enum class FeedbackTone { NEUTRAL, GOOD, WARNING }

data class AnalyzerResult(val message: String, val repCount: Int, val tone: FeedbackTone)

/**
 * 운동 하나를 담당하는 자세 판정 로직의 공통 계약.
 * MoveNet 키포인트를 받아 피드백 문구/반복 횟수를 계산한다.
 * 새 운동을 추가할 때는 이 인터페이스를 구현하는 클래스를 하나 더 만들고
 * [analyzerFor]의 매핑에 등록하면 된다.
 */
interface ExerciseAnalyzer {
    /** keypoints: MoveNet 출력 그대로, 각 원소 = [y, x, score] (0..1 정규화) */
    fun analyze(keypoints: Array<FloatArray>): AnalyzerResult
}

/** 아직 전용 판정 로직이 없는 운동에 대한 자리 표시자 */
class ComingSoonAnalyzer : ExerciseAnalyzer {
    override fun analyze(keypoints: Array<FloatArray>): AnalyzerResult =
        AnalyzerResult("이 운동은 자세 분석을 준비 중입니다.", 0, FeedbackTone.NEUTRAL)
}

// 운동 이름(WorkoutRoutine.name) → 분석기 생성 함수. 이 맵이 "자세 분석을 지원하는 운동"의 유일한 기준이 되도록
// SUPPORTED_EXERCISE_NAMES와 analyzerFor()가 모두 여기서 파생되게 했다 (두 곳에 이름을 따로 적어두면 어긋날 수 있어서).
private val analyzerFactories: Map<String, () -> ExerciseAnalyzer> = linkedMapOf(
    "스쿼트" to { SquatAnalyzer() },
    "런지" to { LungeAnalyzer() },
    "사이드 레터럴 레이즈" to { SideLateralRaiseAnalyzer() },
    "밀리터리 프레스" to { MilitaryPressAnalyzer() },
    "해머 컬" to { HammerCurlAnalyzer() },
    "케이블 푸쉬다운" to { CablePushdownAnalyzer() },
    "덤벨 트라이셉스 익스텐션" to { TricepsExtensionAnalyzer() },
    "바벨 로우" to { BarbellRowAnalyzer() },
    "티바 로우" to { TBarRowAnalyzer() },
    "풀업" to { PullUpAnalyzer() }
)

/** 자세 분석을 지원하는 운동 이름 목록 (등록 순서 유지) */
val SUPPORTED_EXERCISE_NAMES: List<String> = analyzerFactories.keys.toList()

/** 운동 이름(WorkoutRoutine.name)에 맞는 분석기를 고른다. 지원하지 않는 운동은 ComingSoonAnalyzer로 대체. */
fun analyzerFor(exerciseName: String?): ExerciseAnalyzer =
    analyzerFactories[exerciseName]?.invoke() ?: ComingSoonAnalyzer()
