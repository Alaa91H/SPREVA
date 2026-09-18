package com.spreva.core.content

import com.spreva.core.model.CefrLevel
import com.spreva.core.model.LessonId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the bundled content parser and validator (plan
 * sections 198/351). The demo lesson ships as a JVM resource, so the exact
 * bytes the app bundles are exercised here.
 */
class ContentParserTest {

    private val parser = ContentParser()

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()

    @Test
    fun `manifest parses and pins supported schema`() {
        val manifest = parser.parseManifest(resource("content/manifest.json"))

        assertEquals(ContentParser.SUPPORTED_SCHEMA_VERSION, manifest.schemaVersion)
        assertTrue(manifest.contentVersion.isNotBlank())
        assertTrue(manifest.packages.isNotEmpty())
    }

    @Test
    fun `demo course parses with A1 level`() {
        val course = parser.toCourse(parser.parseCourse(resource("content/courses/de-core/course.json")))

        assertEquals("de-core", course.id.value)
        assertEquals(1, course.levels.size)
        assertEquals(CefrLevel.A1, course.levels.first().cefr)
        assertTrue(course.levels.first().units.isNotEmpty())
    }

    @Test
    fun `demo lesson parses with all activity types`() {
        val lesson = parser.parseLesson(resource("content/courses/de-core/lessons/a1_u01_l01.json"))
            .let(parser::toLesson)

        assertEquals(LessonId("a1_u01_l01"), lesson.id)
        assertTrue(lesson.activities.size >= 4)
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.VocabularyIntro })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.MultipleChoice })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.Cloze })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.ListeningChoice })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.LessonSummaryActivity })
    }

    @Test
    fun `listening choice activity maps audio and options`() {
        val lesson = parser.parseLesson(resource("content/courses/de-core/lessons/a1_u01_l01.json"))
            .let(parser::toLesson)

        val listening = lesson.activities
            .filterIsInstance<com.spreva.core.model.LearningActivity.ListeningChoice>()
            .first()
        assertEquals("audio/cc-by-sa/de-wie-heisst-du.ogg", listening.audio)
        assertEquals(3, listening.options.size)
        assertTrue(listening.options.any { it.id == listening.correctOptionId })
    }

    @Test(expected = ContentValidationException::class)
    fun `listening choice without audio is rejected`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "activities": [
              {"id": "a1", "type": "listening_choice", "options": [
                {"id": "o1", "text": {"de": "a"}},
                {"id": "o2", "text": {"de": "b"}},
                {"id": "o3", "text": {"de": "c"}}
              ], "correctOptionId": "o2"}
            ]}
        """
        parser.parseLesson(json.trimIndent()).let(parser::toLesson)
    }

    @Test
    fun `validated demo lesson has no structural errors`() {
        val lesson = parser.parseLesson(resource("content/courses/de-core/lessons/a1_u01_l01.json"))
            .let(parser::toLesson)

        assertEquals(emptyList<String>(), ContentValidator.validateLesson(lesson))
    }

    @Test(expected = ContentValidationException::class)
    fun `unsupported schema version is rejected`() {
        parser.parseManifest("""{"schemaVersion": 99, "contentVersion": "x", "packages": []}""")
    }

    @Test(expected = ContentValidationException::class)
    fun `unknown cefr level is rejected`() {
        ContentParser.parseCefr("Z9")
    }
}
