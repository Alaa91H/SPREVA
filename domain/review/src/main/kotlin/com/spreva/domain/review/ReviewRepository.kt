package com.spreva.domain.review

import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import com.spreva.core.model.ReviewRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persistence contract for review cards and logs. */
interface ReviewRepository {
    /** Due cards ordered by due date, reacting to DB changes. */
    fun observeDueCards(nowEpochMs: Long): Flow<List<ReviewCard>>

    /** Current due count, reacting to DB changes. */
    fun observeDueCount(nowEpochMs: Long): Flow<Int>

    suspend fun getCard(id: ReviewCardId): ReviewCard?

    /** Persists the graded card and appends a review log row. */
    suspend fun saveGradedCard(card: ReviewCard, rating: ReviewRating, reviewedAtEpochMs: Long, durationMs: Long)

    suspend fun insertCardsIfAbsent(cards: List<ReviewCard>): Int

    suspend fun hasCardsForKnowledgeItems(knowledgeItemIds: List<String>): Boolean

    suspend fun clearAll()
}

/** Snapshot of the review queue for UI. */
data class ReviewQueue(
    val dueCards: List<ReviewCard>,
)

/** Observes the due review queue. */
class GetDueReviews @javax.inject.Inject constructor(private val repository: ReviewRepository) {

    operator fun invoke(nowEpochMs: Long): Flow<ReviewQueue> =
        repository.observeDueCards(nowEpochMs).map(::ReviewQueue)
}

/**
 * Grades a card through the scheduler and persists the result.
 * The scheduler implementation is injected so FSRS can replace the demo
 * scheduler without touching this use case.
 */
class GradeReview @javax.inject.Inject constructor(
    private val repository: ReviewRepository,
    private val scheduler: com.spreva.core.memory.ReviewScheduler,
) {

    suspend operator fun invoke(
        card: ReviewCard,
        rating: ReviewRating,
        nowEpochMs: Long,
        durationMs: Long,
    ): ReviewCard {
        val graded = scheduler.grade(card, rating, java.time.Instant.ofEpochMilli(nowEpochMs))
        repository.saveGradedCard(graded, rating, nowEpochMs, durationMs)
        return graded
    }
}


/** Preview-only scheduling options; does not mutate the card or database. */
class PreviewReview @javax.inject.Inject constructor(
    private val scheduler: com.spreva.core.memory.ReviewScheduler,
) {
    operator fun invoke(
        card: ReviewCard,
        nowEpochMs: Long,
    ): Map<ReviewRating, java.time.Instant> =
        scheduler.preview(card, java.time.Instant.ofEpochMilli(nowEpochMs))
}
