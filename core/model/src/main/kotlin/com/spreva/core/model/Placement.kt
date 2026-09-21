package com.spreva.core.model

data class PlacementQuestion(
    val id: String,
    val level: CefrLevel,
    val prompt: LocalizedText,
    val question: LocalizedText,
    val options: List<ChoiceOption>,
    val correctOptionId: String,
)

data class PlacementAnswer(
    val questionId: String,
    val level: CefrLevel,
    val correct: Boolean,
)

data class PlacementDecision(
    val nextLevel: CefrLevel?,
    val recommendedLevel: CefrLevel?,
    val finished: Boolean,
    val confidencePercent: Int,
    val answeredCount: Int,
)
