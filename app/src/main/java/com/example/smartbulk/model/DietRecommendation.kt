package com.example.smartbulk.model

/** Firebase Cloud Function(getDietRecommendation)이 돌려주는 하루 식단 추천 결과. */
data class DietRecommendation(
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val snack: String,
    val note: String
) {
    fun toDisplayText(): String {
        val lines = mutableListOf(
            "🍽 오늘의 식단 🍽",
            "• 아침: $breakfast",
            "• 점심: $lunch",
            "• 저녁: $dinner"
        )
        if (snack.isNotBlank()) lines.add("• 간식: $snack")
        if (note.isNotBlank()) {
            lines.add("")
            lines.add(note)
        }
        return lines.joinToString("\n")
    }
}
