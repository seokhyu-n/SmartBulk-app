package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val PU_LEFT_SHOULDER = 5
private const val PU_LEFT_ELBOW = 7
private const val PU_LEFT_WRIST = 9
private const val PU_RIGHT_SHOULDER = 6
private const val PU_RIGHT_ELBOW = 8
private const val PU_RIGHT_WRIST = 10

private enum class PullUpState { HANGING, PULLED }

/**
 * 풀업: 어깨-팔꿈치-손목 각도로 판정한다.
 * 팔을 편 채 매달린 자세(큰 각도)에서, 턱이 봉 위로 올라오도록 팔꿈치를 굽히면(작은 각도) 1회.
 *
 * ⚠️ 각도 값은 임시 추정치입니다. 실기 테스트로 튜닝이 필요합니다.
 * ⚠️ 카메라가 몸 전체(특히 팔)를 아래에서 위로 잡기 어려운 구도라, 다른 운동보다
 * 카메라 위치/거리에 더 민감할 수 있습니다.
 */
class PullUpAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val pulledAngle: Double = 60.0,    // 이 이하: 턱걸이 정점(작업 자세)
    private val hangingAngle: Double = 160.0,  // 이 이상: 팔을 편 매달린 시작 자세
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = PullUpState.HANGING

    var repCount = 0
        private set

    override fun analyze(keypoints: Array<FloatArray>): AnalyzerResult {
        val arm = pickVisibleArm(keypoints)
            ?: return AnalyzerResult("사람을 인식하지 못했습니다.", repCount, FeedbackTone.NEUTRAL)

        val angle = jointAngle(arm.shoulder, arm.elbow, arm.wrist)
        angleHistory.addLast(angle)
        if (angleHistory.size > maxHistory) angleHistory.removeFirst()
        val avgAngle = angleHistory.average()

        updateRepState(avgAngle)

        val (message, tone) = feedbackFor(avgAngle, arm.side)
        return AnalyzerResult(message, repCount, tone)
    }

    private data class Arm(val shoulder: FloatArray, val elbow: FloatArray, val wrist: FloatArray, val side: String)

    private fun pickVisibleArm(kp: Array<FloatArray>): Arm? {
        fun visible(a: FloatArray, b: FloatArray, c: FloatArray) =
            a[2] > confidenceThreshold && b[2] > confidenceThreshold && c[2] > confidenceThreshold

        val leftShoulder = kp[PU_LEFT_SHOULDER]; val leftElbow = kp[PU_LEFT_ELBOW]; val leftWrist = kp[PU_LEFT_WRIST]
        val rightShoulder = kp[PU_RIGHT_SHOULDER]; val rightElbow = kp[PU_RIGHT_ELBOW]; val rightWrist = kp[PU_RIGHT_WRIST]

        return when {
            visible(leftShoulder, leftElbow, leftWrist) ->
                Arm(xy(leftShoulder), xy(leftElbow), xy(leftWrist), "왼쪽")
            visible(rightShoulder, rightElbow, rightWrist) ->
                Arm(xy(rightShoulder), xy(rightElbow), xy(rightWrist), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 매달린 자세(HANGING) <-> 당겨 올라간 자세(PULLED)를 한 번 왕복해야 1회 인정
        when {
            avgAngle >= hangingAngle -> {
                if (state == PullUpState.PULLED) repCount++
                state = PullUpState.HANGING
            }
            avgAngle <= pulledAngle -> state = PullUpState.PULLED
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle <= pulledAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < hangingAngle -> "[$side] 턱이 봉 위로 올라올 때까지 당기세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
