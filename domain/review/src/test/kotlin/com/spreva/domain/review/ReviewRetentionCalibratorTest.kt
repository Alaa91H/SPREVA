package com.spreva.domain.review

import com.spreva.core.model.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRetentionCalibratorTest {

    private val calibrator = ReviewRetentionCalibrator()

    @Test
    fun `does not personalize with sparse history`() {
        assertNull(calibrator.recommend(List(49) { ReviewRating.GOOD }))
    }

    @Test
    fun `high lapse rate raises desired retention conservatively`() {
        val ratings = List(20) { ReviewRating.AGAIN } + List(80) { ReviewRating.GOOD }
        val result = calibrator.recommend(ratings)!!
        assertEquals(0.95, result.target, 0.0)
        assertEquals(100, result.sampleSize)
    }

    @Test
    fun `very strong recall can reduce workload without leaving safe range`() {
        val ratings = List(4) { ReviewRating.AGAIN } +
            List(20) { ReviewRating.EASY } +
            List(76) { ReviewRating.GOOD }
        val result = calibrator.recommend(ratings)!!
        assertTrue(result.target in ReviewRetentionCalibrator.MIN_TARGET..ReviewRetentionCalibrator.MAX_TARGET)
        assertEquals(0.88, result.target, 0.0)
    }

    @Test
    fun `ordinary performance stays near default target`() {
        val ratings = List(9) { ReviewRating.AGAIN } +
            List(16) { ReviewRating.HARD } +
            List(65) { ReviewRating.GOOD } +
            List(10) { ReviewRating.EASY }
        val result = calibrator.recommend(ratings)!!
        assertEquals(0.90, result.target, 0.0)
    }

    @Test
    fun `only latest bounded history is used`() {
        val newest = List(200) { ReviewRating.GOOD }
        val ancient = List(500) { ReviewRating.AGAIN }
        val result = calibrator.recommend(newest + ancient)!!
        assertEquals(200, result.sampleSize)
        assertEquals(0.87, result.target, 0.0)
    }
}
