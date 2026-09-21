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
    fun `course lesson refs preserve localized title and activity count`() {
        val json = """
            {
              "id": "de-core",
              "title": {"de": "Deutsch", "ar": "الألمانية", "en": "German"},
              "levels": [{
                "id": "de-core-c1",
                "cefr": "C1",
                "title": {"de": "C1", "ar": "C1", "en": "C1"},
                "units": [{
                  "id": "de-core-c1-u01",
                  "title": {"de": "Diskurs", "ar": "الخطاب", "en": "Discourse"},
                  "lessons": [{
                    "id": "c1_u01_l01",
                    "title": {"de": "Informationsstruktur", "ar": "بنية المعلومات", "en": "Information structure"},
                    "activityCount": 7
                  }]
                }]
              }]
            }
        """.trimIndent()

        val course = parser.toCourse(parser.parseCourse(json))
        val summary = course.levels.single().units.single().lessons.single()

        assertEquals("c1_u01_l01", summary.id.value)
        assertEquals("Informationsstruktur", summary.title.de)
        assertEquals("بنية المعلومات", summary.title.ar)
        assertEquals("Information structure", summary.title.en)
        assertEquals(7, summary.activityCount)
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

    @Test
    fun `listening choice can use hidden text as local TTS fallback`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "canDo": ["X"], "activities": [
              {"id": "a1", "type": "listening_choice",
               "text": {"de": "Der Termin ist um neun Uhr.", "ar": "x", "en": "x"},
               "options": [
                {"id": "o1", "text": {"de": "acht", "ar": "acht", "en": "acht"}},
                {"id": "o2", "text": {"de": "neun", "ar": "neun", "en": "neun"}}
              ], "correctOptionId": "o2"}
            ]}
        """
        val lesson = parser.parseLesson(json.trimIndent()).let(parser::toLesson)
        val listening = lesson.activities.single() as com.spreva.core.model.LearningActivity.ListeningChoice
        assertEquals(null, listening.audio)
        assertEquals("Der Termin ist um neun Uhr.", listening.text?.de)
        assertEquals(emptyList<String>(), ContentValidator.validateLesson(lesson))
    }

    @Test
    fun `listening choice without audio or fallback text is structurally rejected`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "canDo": ["X"], "activities": [
              {"id": "a1", "type": "listening_choice", "options": [
                {"id": "o1", "text": {"de": "a", "ar": "a", "en": "a"}},
                {"id": "o2", "text": {"de": "b", "ar": "b", "en": "b"}}
              ], "correctOptionId": "o2"}
            ]}
        """
        val lesson = parser.parseLesson(json.trimIndent()).let(parser::toLesson)
        assertTrue(ContentValidator.validateLesson(lesson).any { it.contains("needs audio or fallback text") })
    }

    @Test
    fun `production activity types parse and validate`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "canDo": ["X"], "activities": [
              {"id": "d1", "type": "dictation",
               "prompt": {"de": "Hören", "ar": "استمع", "en": "Listen"},
               "text": {"de": "Ich komme morgen.", "ar": "x", "en": "x"},
               "acceptedAnswers": ["Ich komme morgen."]},
              {"id": "w1", "type": "free_write",
               "prompt": {"de": "Schreiben", "ar": "اكتب", "en": "Write"},
               "minWords": 30,
               "checklist": [{"de": "Aufgabe", "ar": "المهمة", "en": "Task"}]},
              {"id": "s1", "type": "speaking_prompt",
               "prompt": {"de": "Sprechen", "ar": "تحدث", "en": "Speak"},
               "minSeconds": 30,
               "checklist": [{"de": "Struktur", "ar": "البنية", "en": "Structure"}]}
            ]}
        """
        val lesson = parser.parseLesson(json.trimIndent()).let(parser::toLesson)
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.Dictation })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.FreeWrite })
        assertTrue(lesson.activities.any { it is com.spreva.core.model.LearningActivity.SpeakingPrompt })
        assertEquals(emptyList<String>(), ContentValidator.validateLesson(lesson))
    }

    @Test
    fun `validated demo lesson has no structural errors`() {
        val lesson = parser.parseLesson(resource("content/courses/de-core/lessons/a1_u01_l01.json"))
            .let(parser::toLesson)

        assertEquals(emptyList<String>(), ContentValidator.validateLesson(lesson))
    }

    @Test
    fun `media licenses registry parses from bundled content`() {
        val registry = parser.parseMediaLicenses(resource("content/audio/licenses.json"))

        assertEquals(1, registry.registryVersion)
        assertTrue(registry.files.isNotEmpty())
        registry.files.forEach { file ->
            assertTrue("${file.path} missing attribution", file.attribution.isNotBlank())
            assertTrue(
                "${file.path} unexpected license ${file.license}",
                file.license.startsWith("CC BY-SA"),
            )
        }
    }

    @Test
    fun `unit 2 lesson parses numbers vocab with audio and speaking repeat`() {
        val lesson = parser.parseLesson(resource("content/courses/de-core/lessons/a1_u02_l01.json"))
            .let(parser::toLesson)

        assertEquals(LessonId("a1_u02_l01"), lesson.id)
        val vocab = lesson.activities
            .filterIsInstance<com.spreva.core.model.LearningActivity.VocabularyIntro>()
            .first()
        assertEquals(11, vocab.words.size)
        assertTrue(vocab.words.all { it.audio != null })
        val speaking = lesson.activities
            .filterIsInstance<com.spreva.core.model.LearningActivity.SpeakingRepeat>()
            .first()
        assertEquals("audio/cc-by-sa/de-uhr.ogg", speaking.audio)
        assertEquals("die Uhr", speaking.text.de)
    }

    @Test
    fun `speaking repeat supports local TTS fallback without bundled audio`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "canDo": ["X"], "activities": [
              {"id": "a1", "type": "speaking_repeat",
               "text": {"de": "Ich spreche langsam.", "ar": "x", "en": "x"}}
            ]}
        """
        val lesson = parser.parseLesson(json.trimIndent()).let(parser::toLesson)
        val speaking = lesson.activities.single() as com.spreva.core.model.LearningActivity.SpeakingRepeat
        assertEquals(null, speaking.audio)
        assertEquals("Ich spreche langsam.", speaking.text.de)
        assertEquals(emptyList<String>(), ContentValidator.validateLesson(lesson))
    }

    @Test(expected = ContentValidationException::class)
    fun `speaking repeat without text is rejected`() {
        val json = """
            {"id": "l1", "unitId": "u1", "title": {"de": "t"}, "activities": [
              {"id": "a1", "type": "speaking_repeat", "audio": "audio/x.ogg"}
            ]}
        """
        parser.parseLesson(json.trimIndent()).let(parser::toLesson)
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
