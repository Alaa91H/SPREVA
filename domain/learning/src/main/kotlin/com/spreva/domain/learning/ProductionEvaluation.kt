package com.spreva.domain.learning

import com.spreva.core.common.IdGenerator
import com.spreva.core.model.CefrLevel
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.LearningEventId
import com.spreva.core.model.LessonId
import com.spreva.core.model.LocalizedText
import com.spreva.core.model.ProductionMode
import com.spreva.core.model.ProductionRubric
import com.spreva.core.model.ProductionRubricCriterion
import com.spreva.core.model.ProductionSelfAssessment
import com.spreva.core.model.RubricRating
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * CEFR-oriented rubric catalogue for learner self-review.
 *
 * The same four conceptual criteria recur across levels, but the guidance
 * becomes more demanding from A1 to C1. This is a reflection scaffold,
 * not an automated language-quality grader.
 */
class ProductionRubricFactory @Inject constructor() {

    fun forActivity(lessonId: LessonId, activity: LearningActivity): ProductionRubric? {
        val mode = when (activity) {
            is LearningActivity.FreeWrite -> ProductionMode.WRITING
            is LearningActivity.SpeakingPrompt -> ProductionMode.SPEAKING
            else -> return null
        }
        return build(levelFromLessonId(lessonId), mode)
    }

    fun build(level: CefrLevel, mode: ProductionMode): ProductionRubric {
        require(level in SUPPORTED_LEVELS) { "Unsupported production rubric level: $level" }
        val criteria = when (mode) {
            ProductionMode.WRITING -> writingCriteria(level)
            ProductionMode.SPEAKING -> speakingCriteria(level)
        }
        return ProductionRubric(level = level, mode = mode, criteria = criteria)
    }

    private fun writingCriteria(level: CefrLevel): List<ProductionRubricCriterion> = listOf(
        criterion(
            "task",
            "Aufgabenerfüllung", "إنجاز المهمة", "Task fulfilment",
            writingTask(level),
        ),
        criterion(
            "organization",
            "Aufbau & Kohärenz", "التنظيم والترابط", "Organization & coherence",
            writingOrganization(level),
        ),
        criterion(
            "control",
            "Sprachliche Kontrolle", "التحكم اللغوي", "Language control",
            writingControl(level),
        ),
        criterion(
            "lexis",
            "Wortschatz & Präzision", "المفردات والدقة", "Vocabulary & precision",
            writingLexis(level),
        ),
    )

    private fun speakingCriteria(level: CefrLevel): List<ProductionRubricCriterion> = listOf(
        criterion(
            "task",
            "Aufgabenerfüllung", "إنجاز المهمة", "Task fulfilment",
            speakingTask(level),
        ),
        criterion(
            "flow",
            "Sprechfluss & Struktur", "الطلاقة والتنظيم", "Fluency & organization",
            speakingFlow(level),
        ),
        criterion(
            "control",
            "Sprachliche Kontrolle", "التحكم اللغوي", "Language control",
            speakingControl(level),
        ),
        criterion(
            "intelligibility",
            "Verständlichkeit & Aussprache", "الوضوح والنطق", "Intelligibility & pronunciation",
            speakingIntelligibility(level),
        ),
    )

    private fun criterion(
        id: String,
        de: String,
        ar: String,
        en: String,
        guidance: Triple<String, String, String>,
    ) = ProductionRubricCriterion(
        id = id,
        title = LocalizedText(de = de, ar = ar, en = en),
        guidance = LocalizedText(de = guidance.first, ar = guidance.second, en = guidance.third),
    )

    private fun writingTask(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Alle verlangten Basisinformationen sind enthalten.", "أدرجت جميع المعلومات الأساسية المطلوبة.", "All requested basic information is included.")
        CefrLevel.A2 -> tri("Die Hauptpunkte der Alltagssituation sind vollständig bearbeitet.", "عالجت النقاط الأساسية للموقف اليومي كاملة.", "The main points of the everyday task are fully covered.")
        CefrLevel.B1 -> tri("Aufgabe, Absicht und relevante Details sind klar und ausreichend entwickelt.", "المهمة والهدف والتفاصيل المهمة واضحة ومطوّرة بما يكفي.", "Task, purpose and relevant details are clear and sufficiently developed.")
        CefrLevel.B2 -> tri("Die Aufgabe wird differenziert bearbeitet; Begründung und relevante Einschränkungen sind erkennbar.", "عالجت المهمة بتفصيل مع تبرير وقيود ذات صلة.", "The task is handled with nuance; reasoning and relevant limitations are visible.")
        CefrLevel.C1 -> tri("Komplexe Anforderungen werden vollständig, präzise und adressatengerecht erfüllt.", "لبيت المتطلبات المعقدة كاملة وبدقة وبما يناسب المتلقي.", "Complex requirements are fulfilled fully, precisely and appropriately for the audience.")
        else -> error("unsupported")
    }

    private fun writingOrganization(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Kurze Sätze stehen in einer verständlichen Reihenfolge.", "الجمل القصيرة مرتبة بصورة مفهومة.", "Short sentences appear in an understandable order.")
        CefrLevel.A2 -> tri("Sätze sind mit einfachen Konnektoren verbunden; Anfang und Ende sind erkennbar.", "ربطت الجمل بروابط بسيطة وهناك بداية ونهاية واضحتان.", "Sentences use simple connectors with a recognizable beginning and ending.")
        CefrLevel.B1 -> tri("Absätze und Konnektoren führen den Leser nachvollziehbar durch den Text.", "الفقرات والروابط تقود القارئ بوضوح عبر النص.", "Paragraphs and connectors guide the reader coherently through the text.")
        CefrLevel.B2 -> tri("Argumente und Beispiele sind logisch gewichtet und kohärent verknüpft.", "الحجج والأمثلة موزونة منطقيًا ومترابطة.", "Arguments and examples are logically weighted and coherently linked.")
        CefrLevel.C1 -> tri("Informationsstruktur, Übergänge und Fokus steuern einen komplexen Text souverän.", "بنية المعلومات والانتقالات والبؤرة تدير نصًا معقدًا بتمكن.", "Information structure, transitions and focus control a complex text effectively.")
        else -> error("unsupported")
    }

    private fun writingControl(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Einfache Verbformen und Wortstellung sind meist verständlich.", "صيغ الأفعال البسيطة وترتيب الكلمات مفهومان في الغالب.", "Basic verb forms and word order are mostly understandable.")
        CefrLevel.A2 -> tri("Häufige Strukturen, Fälle und Satzklammern funktionieren überwiegend.", "التراكيب الشائعة والحالات وإطار الجملة صحيحة في الغالب.", "Frequent structures, cases and sentence brackets mostly work.")
        CefrLevel.B1 -> tri("Haupt- und Nebensätze sowie zentrale Zeit-/Kasusformen sind ausreichend kontrolliert.", "الجمل الرئيسية والتابعة وأهم الأزمنة والحالات مضبوطة بما يكفي.", "Main/subordinate clauses and core tense/case forms are sufficiently controlled.")
        CefrLevel.B2 -> tri("Komplexe Strukturen werden mit wenigen störenden Fehlern eingesetzt.", "تستخدم تراكيب معقدة مع أخطاء قليلة تعيق الفهم.", "Complex structures are used with few errors that interfere with meaning.")
        CefrLevel.C1 -> tri("Grammatik und Syntax bleiben auch bei hoher Komplexität flexibel und weitgehend sicher.", "تبقى القواعد والنحو مرنين وآمنين بدرجة كبيرة حتى مع التعقيد.", "Grammar and syntax remain flexible and largely secure even at high complexity.")
        else -> error("unsupported")
    }

    private fun writingLexis(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Der Grundwortschatz passt zur Situation; wichtige Wörter fehlen nicht.", "المفردات الأساسية مناسبة للموقف ولا تنقص كلمات رئيسية.", "Basic vocabulary fits the situation and key words are present.")
        CefrLevel.A2 -> tri("Alltagswortschatz wird passend variiert; Wiederholungen stören nicht stark.", "تنوع مفردات الحياة اليومية مناسب والتكرار غير مزعج كثيرًا.", "Everyday vocabulary is varied appropriately without disruptive repetition.")
        CefrLevel.B1 -> tri("Wortwahl ist meist präzise genug für Beschreibung, Meinung und Begründung.", "اختيار الكلمات دقيق غالبًا للوصف والرأي والتبرير.", "Word choice is generally precise enough for description, opinion and reasoning.")
        CefrLevel.B2 -> tri("Kollokationen, Register und präzisere Alternativen werden bewusst eingesetzt.", "تستخدم المتلازمات والسجل والبدائل الدقيقة بوعي.", "Collocations, register and more precise alternatives are used deliberately.")
        CefrLevel.C1 -> tri("Wortwahl, Kollokationen, Register und Nuancen sind flexibel und präzise gesteuert.", "المفردات والمتلازمات والسجل والفروق الدقيقة مضبوطة بمرونة ودقة.", "Lexis, collocations, register and nuance are controlled flexibly and precisely.")
        else -> error("unsupported")
    }

    private fun speakingTask(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Die verlangten Basisinformationen werden verständlich genannt.", "ذكرت المعلومات الأساسية المطلوبة بصورة مفهومة.", "The requested basic information is communicated understandably.")
        CefrLevel.A2 -> tri("Die Alltagssituation wird mit den wichtigsten Punkten bewältigt.", "تعاملت مع الموقف اليومي بأهم النقاط.", "The everyday situation is handled with its key points.")
        CefrLevel.B1 -> tri("Die Aussage ist zusammenhängend und enthält Erklärung oder Begründung.", "الكلام مترابط ويتضمن شرحًا أو تبريرًا.", "The response is connected and includes explanation or reasoning.")
        CefrLevel.B2 -> tri("Position, Begründung, Beispiel und Einschränkung werden differenziert entwickelt.", "طورت الموقف والتبرير والمثال والقيد بصورة متمايزة.", "Position, reasoning, example and limitation are developed with nuance.")
        CefrLevel.C1 -> tri("Komplexe Inhalte werden adressatengerecht, präzise und rhetorisch kontrolliert vermittelt.", "تنقل محتوى معقدًا بدقة وبما يناسب المتلقي مع تحكم خطابي.", "Complex content is conveyed precisely, audience-appropriately and with rhetorical control.")
        else -> error("unsupported")
    }

    private fun speakingFlow(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Kurze Äußerungen gelingen mit sinnvollen Pausen.", "تنجح العبارات القصيرة مع وقفات مناسبة.", "Short utterances work with sensible pauses.")
        CefrLevel.A2 -> tri("Mehrere Sätze werden mit einfachen Verknüpfungen zusammenhängend gesprochen.", "تربط عدة جمل بروابط بسيطة بصورة مترابطة.", "Several sentences are connected with simple linking.")
        CefrLevel.B1 -> tri("Die Äußerung bleibt über längere Passagen zusammenhängend; Pausen helfen der Struktur.", "يبقى الكلام مترابطًا لفترات أطول والوقفات تخدم التنظيم.", "The response stays connected over longer stretches and pauses support structure.")
        CefrLevel.B2 -> tri("Tempo, Pausen und Konnektoren tragen eine klare Argumentstruktur.", "السرعة والوقفات والروابط تدعم بنية حجاج واضحة.", "Pace, pauses and connectors support a clear argument structure.")
        CefrLevel.C1 -> tri("Tempo, Fokus, Pausen und Übergänge werden flexibel zur Diskurssteuerung eingesetzt.", "تستخدم السرعة والبؤرة والوقفات والانتقالات بمرونة لإدارة الخطاب.", "Pace, focus, pauses and transitions are used flexibly to manage discourse.")
        else -> error("unsupported")
    }

    private fun speakingControl(level: CefrLevel) = writingControl(level)

    private fun speakingIntelligibility(level: CefrLevel) = when (level) {
        CefrLevel.A1 -> tri("Schlüsselwörter, Vokale und Wortakzent sind ausreichend verständlich.", "الكلمات الأساسية والحركات ونبر الكلمات مفهومة بما يكفي.", "Key words, vowels and word stress are sufficiently intelligible.")
        CefrLevel.A2 -> tri("Endungen, Satzakzent und Frageintonation bleiben meist gut verständlich.", "النهايات ونبر الجملة ولحن السؤال واضحة في الغالب.", "Endings, sentence stress and question intonation are mostly clear.")
        CefrLevel.B1 -> tri("Rhythmus, Sprechgruppen und Kontrastakzent unterstützen das Verstehen.", "الإيقاع ومجموعات الكلام والنبر المقارن تدعم الفهم.", "Rhythm, thought groups and contrastive stress support understanding.")
        CefrLevel.B2 -> tri("Prosodie und Fokus machen Haltung, Kontrast und Einschränkung hörbar.", "النبر والبؤرة يجعلان الموقف والتباين والقيود مسموعة.", "Prosody and focus make stance, contrast and limitation audible.")
        CefrLevel.C1 -> tri("Prosodie, Tempo und rhetorische Pausen unterstützen Nuance und professionelle Wirkung.", "الإيقاع والسرعة والوقفات البلاغية تدعم الفروق الدقيقة والأثر المهني.", "Prosody, pace and rhetorical pauses support nuance and professional impact.")
        else -> error("unsupported")
    }

    private fun levelFromLessonId(lessonId: LessonId): CefrLevel {
        val prefix = lessonId.value.substringBefore('_').uppercase()
        return runCatching { CefrLevel.valueOf(prefix) }.getOrDefault(CefrLevel.B1)
    }

    private fun tri(de: String, ar: String, en: String) = Triple(de, ar, en)

    companion object {
        val SUPPORTED_LEVELS = setOf(
            CefrLevel.A1,
            CefrLevel.A2,
            CefrLevel.B1,
            CefrLevel.B2,
            CefrLevel.C1,
        )
    }
}

/** Pure scorer for a fully completed rubric. */
class ProductionRubricScorer @Inject constructor() {

    fun score(
        rubric: ProductionRubric,
        ratings: Map<String, RubricRating>,
    ): Int {
        require(rubric.criteria.isNotEmpty())
        require(rubric.criteria.all { ratings.containsKey(it.id) }) {
            "All rubric criteria must be rated"
        }
        val earned = rubric.criteria.sumOf { ratings.getValue(it.id).points }
        val maximum = rubric.criteria.size * RubricRating.CONSISTENT.points
        return (earned * 100.0 / maximum).roundToInt().coerceIn(0, 100)
    }
}

/**
 * Persists only rubric evidence/metadata into the append-only learning log.
 * Learner text and raw audio are deliberately excluded from the payload.
 */
class RecordProductionSelfAssessment @Inject constructor(
    private val eventLog: LearningEventLog,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        assessment: ProductionSelfAssessment,
        now: Instant,
    ) {
        val payload = buildJsonObject {
            put("lessonId", assessment.lessonId.value)
            put("activityId", assessment.activityId.value)
            put("level", assessment.level.name)
            put("mode", assessment.mode.name)
            put("selfScorePercent", assessment.selfScorePercent)
            assessment.wordCount?.let { put("wordCount", it) }
            assessment.durationMs?.let { put("durationMs", it) }
            putJsonObject("ratings") {
                assessment.ratings.forEach { (criterion, rating) ->
                    put(criterion, rating.name)
                }
            }
        }.toString()

        eventLog.log(
            id = LearningEventId(idGenerator.newId()),
            type = EVENT_TYPE,
            subjectId = assessment.activityId.value,
            occurredAt = now,
            payloadJson = payload,
        )
    }

    companion object {
        const val EVENT_TYPE = "ProductionRubricSelfAssessment"
    }
}
