package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val TR_LEFT_SHOULDER = 5
private const val TR_LEFT_ELBOW = 7
private const val TR_LEFT_WRIST = 9
private const val TR_RIGHT_SHOULDER = 6
private const val TR_RIGHT_ELBOW = 8
private const val TR_RIGHT_WRIST = 10

private enum class TRowState { HANGING, PULLED }

/**
 * 티바 로우: 바벨 로우와 동작 패턴은 같지만(어깨-팔꿈치-손목 각도), 그립이 좁고
 * 몸에 더 가까운 궤적이라 별도 클래스로 분리해 독립적으로 튜닝할 수 있게 했다.
 *
 * ⚠️ 각도 값은 임시 추정치입니다. 실기 테스트로 튜닝이 필요합니다.
 */
class TBarRowAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val pulledAngle: Double = 80.0,    // 이 이하: 핸들을 몸통까지 당긴 작업 자세
    private val hangingAngle: Double = 150.0,  // 이 이상: 팔을 늘어뜨린 시작 자세
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = TRowState.HANGING

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

        val leftShoulder = kp[TR_LEFT_SHOULDER]; val leftElbow = kp[TR_LEFT_ELBOW]; val leftWrist = kp[TR_LEFT_WRIST]
        val rightShoulder = kp[TR_RIGHT_SHOULDER]; val rightElbow = kp[TR_RIGHT_ELBOW]; val rightWrist = kp[TR_RIGHT_WRIST]

        return when {
            visible(leftShoulder, leftElbow, leftWrist) ->
                Arm(xy(leftShoulder), xy(leftElbow), xy(leftWrist), "왼쪽")
            visible(rightShoulder, rightElbow, rightWrist) ->
                Arm(xy(rightShoulder), xy(rightElbow), xy(rightWrist), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 늘어뜨린 자세(HANGING) <-> 당긴 자세(PULLED)를 한 번 왕복해야 1회 인정
        when {
            avgAngle >= hangingAngle -> {
                if (state == TRowState.PULLED) repCount++
                state = TRowState.HANGING
            }
            avgAngle <= pulledAngle -> state = TRowState.PULLED
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle <= pulledAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < hangingAngle -> "[$side] 조금 더 당기세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
