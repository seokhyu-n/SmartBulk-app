package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val HC_LEFT_SHOULDER = 5
private const val HC_LEFT_ELBOW = 7
private const val HC_LEFT_WRIST = 9
private const val HC_RIGHT_SHOULDER = 6
private const val HC_RIGHT_ELBOW = 8
private const val HC_RIGHT_WRIST = 10

private enum class CurlState { EXTENDED, CURLED }

/**
 * 해머 컬: 어깨-팔꿈치-손목 각도로 판정한다.
 * 팔을 편 상태(큰 각도)에서 덤벨을 들어 팔꿈치를 굽히면(작은 각도) 1회.
 *
 * ⚠️ 각도 값은 임시 추정치입니다. 실기 테스트로 튜닝이 필요합니다.
 */
class HammerCurlAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val curledAngle: Double = 50.0,    // 이 이하: 완전히 굽힌 자세(작업 자세)
    private val extendedAngle: Double = 150.0, // 이 이상: 팔을 편 시작 자세
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = CurlState.EXTENDED

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

        val leftShoulder = kp[HC_LEFT_SHOULDER]; val leftElbow = kp[HC_LEFT_ELBOW]; val leftWrist = kp[HC_LEFT_WRIST]
        val rightShoulder = kp[HC_RIGHT_SHOULDER]; val rightElbow = kp[HC_RIGHT_ELBOW]; val rightWrist = kp[HC_RIGHT_WRIST]

        return when {
            visible(leftShoulder, leftElbow, leftWrist) ->
                Arm(xy(leftShoulder), xy(leftElbow), xy(leftWrist), "왼쪽")
            visible(rightShoulder, rightElbow, rightWrist) ->
                Arm(xy(rightShoulder), xy(rightElbow), xy(rightWrist), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 편 자세(EXTENDED) <-> 굽힌 자세(CURLED)를 한 번 왕복해야 1회 인정
        when {
            avgAngle >= extendedAngle -> {
                if (state == CurlState.CURLED) repCount++
                state = CurlState.EXTENDED
            }
            avgAngle <= curledAngle -> state = CurlState.CURLED
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle <= curledAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < extendedAngle -> "[$side] 더 굽히세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
