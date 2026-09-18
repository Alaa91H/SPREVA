package com.spreva.core.content

import com.spreva.core.model.Course
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import java.io.InputStream

/**
 * Content source abstraction so downloadable packages can replace/augment
 * the bundled source later (plan sections 195-196) without touching features.
 */
interface ContentSource {
    fun loadManifest(): ContentManifestDto
    fun loadCourse(courseId: String): Course
    fun loadLesson(lessonId: LessonId): Lesson
    fun contentVersion(): String
}

/**
 * Phase 3 source: JSON files shipped inside the app (JVM resources of this
 * module for unit tests; mirrored into app assets for the APK).
 */
class BundledContentSource(
    private val parser: ContentParser = ContentParser(),
    private val resourceOpener: (String) -> InputStream,
) : ContentSource {

    override fun loadManifest(): ContentManifestDto =
        parser.parseManifest(resourceOpener("content/manifest.json").bufferedReader().readText())

    override fun loadCourse(courseId: String): Course =
        parser.toCourse(parser.parseCourse(resourceOpener("content/courses/$courseId/course.json").bufferedReader().readText()))

    override fun loadLesson(lessonId: LessonId): Lesson =
        parser.parseLesson(resourceOpener("content/courses/de-core/lessons/${lessonId.value}.json").bufferedReader().readText())
            .let(parser::toLesson)

    override fun contentVersion(): String = loadManifest().contentVersion
}
