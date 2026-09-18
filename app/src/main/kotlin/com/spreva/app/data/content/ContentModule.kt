package com.spreva.app.data.content

import com.spreva.core.content.BundledContentSource
import com.spreva.core.content.ContentSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * The app ships the demo content inside its assets and opens it via the
 * same content/ layout the JVM resource loader uses, so unit tests and
 * the running app read identical bytes (plan sections 190-193).
 */
@Module
@InstallIn(SingletonComponent::class)
object ContentModule {

    @Provides
    @Singleton
    fun provideContentParser(): com.spreva.core.content.ContentParser =
        com.spreva.core.content.ContentParser()

    @Provides
    @Singleton
    fun provideJson(): Json = com.spreva.core.content.ContentParser.defaultJson

    @Provides
    @Singleton
    fun provideContentSource(parser: com.spreva.core.content.ContentParser): ContentSource =
        BundledContentSource(
            parser = parser,
            resourceOpener = ::openContentResource,
        )

    private fun openContentResource(path: String): java.io.InputStream {
        val normalized = path.removePrefix("content/")
        val stream = ContentModule::class.java.classLoader
            ?.getResourceAsStream("content/$normalized")
        requireNotNull(stream) { "Bundled content resource not found: $normalized" }
        return stream
    }
}
