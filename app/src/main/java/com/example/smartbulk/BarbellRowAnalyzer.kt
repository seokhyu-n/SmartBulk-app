package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val BR_LEFT_SHOULDER = 5
private const val BR_LEFT_ELBOW = 7
private const val BR_LEFT_WRIST = 9
private const val BR_RIGHT_SHOULDER = 6
private const val BR_RIGHT_ELBOW = 8
private const val BR_RIGHT_WRIST = 10

private enum class RowState { HANGING, PULLED }

/**
 * 바벨 로우: 어깨-팔꿈치-손목 각도로 판정한다.
 * 팔을 늘어뜨린 시작 자세(큰 각도)에서, 팔꿈치를 굽혀 바벨을 몸통까지 당기면(작은 각도) 1회.
 *
 * ⚠️ MVP 버전: 상체가 숙여진 각도(허리 자세)는 아직 보지 않는다. 각도 값도 임시 추정치라
 * 실기 테스트로 튜닝이 필요하다.
 */
class BarbellRowAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val pulledAngle: Double = 80.0,    // 이 이하: 바벨을 몸통까지 당긴 작업 자세
    private val hangingAngle: Double = 150.0,  // 이 이상: 팔을 늘어뜨린 시작 자세
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = RowState.HANGING

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

        val leftShoulder = kp[BR_LEFT_SHOULDER]; val leftElbow = kp[BR_LEFT_ELBOW]; val leftWrist = kp[BR_LEFT_WRIST]
        val rightShoulder = kp[BR_RIGHT_SHOULDER]; val rightElbow = kp[BR_RIGHT_ELBOW]; val rightWrist = kp[BR_RIGHT_WRIST]

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
                if (state == RowState.PULLED) repCount++
                state = RowState.HANGING
            }
            avgAngle <= pulledAngle -> state = RowState.PULLED
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle <= pulledAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < hangingAngle -> "[$side] 조금 더 당기세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
