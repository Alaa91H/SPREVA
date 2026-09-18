package com.spreva.app.data

import com.spreva.app.data.OfflineFirstCurriculumRepository
import com.spreva.app.data.OfflineFirstLearningRepository
import com.spreva.app.data.RoomLearningEventLog
import com.spreva.app.data.RoomReviewRepository
import com.spreva.app.data.DefaultReviewCardProvisioner
import com.spreva.domain.curriculum.CurriculumRepository
import com.spreva.domain.learning.LearningEventLog
import com.spreva.domain.learning.LearningRepository
import com.spreva.domain.learning.ReviewCardProvisioner
import com.spreva.domain.review.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the offline-first implementations to their domain contracts. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCurriculumRepository(impl: OfflineFirstCurriculumRepository): CurriculumRepository

    @Binds
    abstract fun bindLearningRepository(impl: OfflineFirstLearningRepository): LearningRepository

    @Binds
    abstract fun bindLearningEventLog(impl: RoomLearningEventLog): LearningEventLog

    @Binds
    abstract fun bindReviewCardProvisioner(impl: DefaultReviewCardProvisioner): ReviewCardProvisioner

    @Binds
    abstract fun bindReviewRepository(impl: RoomReviewRepository): ReviewRepository
}
