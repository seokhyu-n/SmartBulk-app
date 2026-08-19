package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val LR_LEFT_HIP = 11
private const val LR_LEFT_SHOULDER = 5
private const val LR_LEFT_ELBOW = 7
private const val LR_RIGHT_HIP = 12
private const val LR_RIGHT_SHOULDER = 6
private const val LR_RIGHT_ELBOW = 8

private enum class RaiseState { RESTING, RAISED }

/**
 * 사이드 레터럴 레이즈: 팔이 몸통(엉덩이-어깨-팔꿈치) 기준 얼마나 벌어졌는지로 판정한다.
 * 팔이 옆으로 내려가 있으면 각도가 작고, 어깨 높이까지 들어 올리면 90도 근처가 된다.
 *
 * ⚠️ 각도 값은 전부 임시 추정치입니다. 스쿼트 때처럼 실제 폰으로 촬영하면서
 * Log로 각도를 찍어보고 튜닝이 필요합니다.
 */
class SideLateralRaiseAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val restAngle: Double = 30.0,     // 이 이하: 팔이 몸통에 붙은 상태(휴식)
    private val raisedAngle: Double = 70.0,   // 이 이상: 어깨 높이까지 올라온 상태(작업 자세)
    private val overRaiseAngle: Double = 110.0, // 이 이상: 너무 높이 든 상태(어깨 무리 위험)
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = RaiseState.RESTING

    var repCount = 0
        private set

    override fun analyze(keypoints: Array<FloatArray>): AnalyzerResult {
        val arm = pickVisibleArm(keypoints)
            ?: return AnalyzerResult("사람을 인식하지 못했습니다.", repCount, FeedbackTone.NEUTRAL)

        val angle = jointAngle(arm.hip, arm.shoulder, arm.elbow)
        angleHistory.addLast(angle)
        if (angleHistory.size > maxHistory) angleHistory.removeFirst()
        val avgAngle = angleHistory.average()

        updateRepState(avgAngle)

        val (message, tone) = feedbackFor(avgAngle, arm.side)
        return AnalyzerResult(message, repCount, tone)
    }

    private data class Arm(val hip: FloatArray, val shoulder: FloatArray, val elbow: FloatArray, val side: String)

    private fun pickVisibleArm(kp: Array<FloatArray>): Arm? {
        fun visible(a: FloatArray, b: FloatArray, c: FloatArray) =
            a[2] > confidenceThreshold && b[2] > confidenceThreshold && c[2] > confidenceThreshold

        val leftHip = kp[LR_LEFT_HIP]; val leftShoulder = kp[LR_LEFT_SHOULDER]; val leftElbow = kp[LR_LEFT_ELBOW]
        val rightHip = kp[LR_RIGHT_HIP]; val rightShoulder = kp[LR_RIGHT_SHOULDER]; val rightElbow = kp[LR_RIGHT_ELBOW]

        return when {
            visible(leftHip, leftShoulder, leftElbow) ->
                Arm(xy(leftHip), xy(leftShoulder), xy(leftElbow), "왼쪽")
            visible(rightHip, rightShoulder, rightElbow) ->
                Arm(xy(rightHip), xy(rightShoulder), xy(rightElbow), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 휴식(RESTING) <-> 어깨높이로 든 상태(RAISED)를 한 번 왕복해야 1회 인정
        when {
            avgAngle <= restAngle -> {
                if (state == RaiseState.RAISED) repCount++
                state = RaiseState.RESTING
            }
            avgAngle >= raisedAngle -> state = RaiseState.RAISED
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle >= overRaiseAngle -> "[$side] 너무 높이 올렸습니다, 어깨 높이까지만" to FeedbackTone.WARNING
        avgAngle >= raisedAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle > restAngle -> "[$side] 어깨 높이까지 더 올리세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
