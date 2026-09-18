package com.spreva.core.featureflags

/**
 * Phase 3 feature flags (plan section 110). Local static implementation now;
 * remote config can replace the provider later without touching features.
 */
enum class FeatureFlag {
    DYNAMIC_COLOR,
    EXPERIMENTAL_EXPRESSIVE_COMPONENTS,
    DEMO_REVIEW_SCHEDULER,
}

interface FeatureFlagProvider {
    fun isEnabled(flag: FeatureFlag): Boolean
}

/** Default Phase 3 values. */
class StaticFeatureFlagProvider : FeatureFlagProvider {
    override fun isEnabled(flag: FeatureFlag): Boolean = when (flag) {
        FeatureFlag.DYNAMIC_COLOR -> false
        FeatureFlag.EXPERIMENTAL_EXPRESSIVE_COMPONENTS -> false
        FeatureFlag.DEMO_REVIEW_SCHEDULER -> true
    }
}
