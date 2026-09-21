package com.spreva.feature.lesson.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.spreva.core.audio.AudioFileResolver
import com.spreva.core.audio.CourseAudioPlayer
import com.spreva.core.audio.MediaEnvelopeExtractor
import com.spreva.core.audio.SpeechText
import com.spreva.core.audio.TtsStatus
import com.spreva.core.audio.TtsProvider
import com.spreva.core.audio.VoiceRecorder
import com.spreva.core.audio.WaveformComparator
import com.spreva.core.common.AppClock
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import com.spreva.core.model.ProductionSelfAssessment
import com.spreva.core.model.RubricRating
import com.spreva.domain.curriculum.GetLesson
import com.spreva.domain.learning.CompleteLesson
import com.spreva.domain.learning.LearningRepository
import com.spreva.domain.learning.ProductionRubricFactory
import com.spreva.domain.learning.ProductionRubricScorer
import com.spreva.domain.learning.RecordProductionSelfAssessment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lesson player ViewModel: loads content, grades answers, records attempts
 * and completes the lesson (progress persists across restarts — plan
 * sections 9/243-247). Grading of cloze answers is case-insensitive and
 * trims whitespace/punctuation.
 *
 * Phase 4.1: owns TTS playback for German content (plan sections 31-34).
 * The provider is injected behind an interface; features never touch the
 * engine directly.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getLesson: GetLesson,
    private val learningRepository: LearningRepository,
    private val completeLesson: CompleteLesson,
    private val rubricFactory: ProductionRubricFactory,
    private val rubricScorer: ProductionRubricScorer,
    private val recordProductionSelfAssessment: RecordProductionSelfAssessment,
    private val clock: AppClock,
    private val ttsProvider: TtsProvider,
    private val courseAudioPlayer: CourseAudioPlayer,
    private val voiceRecorder: VoiceRecorder,
    private val audioFileResolver: AudioFileResolver,
    settingsDataSource: SettingsDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    /** Audit §9: instruction-language for translation resolution. */
    val uiLanguage = settingsDataSource.settings
        .map { it.uiLanguage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.spreva.core.model.UiLanguage.ENGLISH)

    val ttsStatus: StateFlow<TtsStatus> = ttsProvider.status

    private var attemptCounter = mutableMapOf<String, Int>()
    private var currentActivityShownAtMs = 0L
    private var lastRecording: java.io.File? = null

    init {
        // Expose German voice availability to the lesson UI.
        viewModelScope.launch {
            ttsProvider.status.collect { availability ->
                _uiState.value = _uiState.value.copy(ttsStatus = availability)
            }
        }
    }

    override fun onCleared() {
        voiceRecorder.cancel()
        ttsProvider.stop()
        courseAudioPlayer.stop()
        // Raw learner audio is temporary by design (privacy, plan section 108).
        lastRecording?.delete()
        super.onCleared()
    }

    /** Plays the German prompt of the current activity via TTS. */
    fun speakCurrent() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex) ?: return
        when (activity) {
            is com.spreva.core.model.LearningActivity.TextIntro ->
                ttsProvider.speakGerman(article = null, text = activity.title.de)

            is com.spreva.core.model.LearningActivity.VocabularyIntro ->
                ttsProvider.speakGerman(article = null, text = SpeechText.clean(activity.words.joinToString(" ") { it.german }))

            is com.spreva.core.model.LearningActivity.MultipleChoice ->
                ttsProvider.speakGerman(article = null, text = activity.question.de)

            is com.spreva.core.model.LearningActivity.ListeningChoice ->
                courseAudioPlayer.playOrSpeak(
                    contentPath = activity.audio,
                    fallbackText = activity.text?.de.orEmpty(),
                )

            is com.spreva.core.model.LearningActivity.Dictation ->
                courseAudioPlayer.playOrSpeak(
                    contentPath = activity.audio,
                    fallbackText = activity.text.de,
                )

            is com.spreva.core.model.LearningActivity.FreeWrite ->
                ttsProvider.speakGerman(article = null, text = activity.prompt.de)

            is com.spreva.core.model.LearningActivity.SpeakingPrompt ->
                ttsProvider.speakGerman(article = null, text = activity.prompt.de)

            is com.spreva.core.model.LearningActivity.Cloze ->
                ttsProvider.speakGerman(
                    article = null,
                    text = SpeechText.forSentence(
                        template = activity.sentenceTemplate.de,
                        answerPlaceholder = activity.answerPlaceholder,
                        answer = null,
                    ),
                )

            is com.spreva.core.model.LearningActivity.LessonSummaryActivity,
            is com.spreva.core.model.LearningActivity.SpeakingRepeat,
            -> Unit
        }
    }

    /**
     * Plays a vocabulary word (Phase 4.2): bundled native-speaker recording
     * first, TTS fallback when no recording is bundled (plan sections 32/65).
     */
    fun speakWord(article: String?, german: String, audioPath: String?) {
        courseAudioPlayer.playOrSpeak(contentPath = audioPath, fallbackText = german, article = article)
    }

    /** Replays the listening-activity recording (Phase 4.2 listening task). */
    fun playListeningAudio() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex)
        if (activity is com.spreva.core.model.LearningActivity.ListeningChoice) {
            courseAudioPlayer.playOrSpeak(
                contentPath = activity.audio,
                fallbackText = activity.text?.de.orEmpty(),
            )
        }
    }

    /** Replays a dictation source without revealing its target text. */
    fun playDictationAudio() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex)
        if (activity is com.spreva.core.model.LearningActivity.Dictation) {
            courseAudioPlayer.playOrSpeak(
                contentPath = activity.audio,
                fallbackText = activity.text.de,
            )
        }
    }

    /**
     * Phase 4.3 shadowing starter: play the model recording, record the
     * learner, then let them compare by ear (self-playback). No raw audio
     * retention — files are temporary (plan sections 101-102/108).
     */
    fun startRecording() {
        courseAudioPlayer.stop()
        val result = voiceRecorder.start(java.io.File(_recordingDir(), "spreva-voice.m4a"))
        _uiState.value = _uiState.value.copy(
            isRecording = result is com.spreva.core.audio.RecordingStartResult.Started,
            // Audit §11: surface the failure instead of crashing the lesson.
            recordingError = (result as? com.spreva.core.audio.RecordingStartResult.Failed)?.reason,
        )
    }

    fun stopRecording() {
        val recording = voiceRecorder.stop()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            hasRecording = recording != null,
            recordingDurationMs = recording?.durationMs,
        )
        lastRecording?.takeIf { it != recording?.file }?.delete()
        lastRecording = recording?.file
        computeSimilarity()
    }

    /**
     * Phase 4.3 completion: automatic waveform comparison between the
     * learner's take and the native model recording. Both files are decoded
     * to PCM envelopes ([MediaEnvelopeExtractor]) and scored with
     * [WaveformComparator.similarity]. Null means "no automatic score" —
     * e.g. a model recording failed to decode — in which case the UI keeps
     * the compare-by-ear guidance (plan section 38: compare → re-record).
     */
    private fun computeSimilarity() {
        val state = _uiState.value
        val modelFile = state.lesson?.activities?.getOrNull(state.currentIndex)
            ?.let { it as? com.spreva.core.model.LearningActivity.SpeakingRepeat }
            ?.audio
            ?.let { audioFileResolver.resolveFile(it) }
        val own = lastRecording ?: return
        _uiState.value = _uiState.value.copy(
            similarityScore = if (modelFile?.isFile == true) {
                val model = MediaEnvelopeExtractor.extract(modelFile)
                val learner = MediaEnvelopeExtractor.extract(own)
                if (model.isNotEmpty() && learner.isNotEmpty()) {
                    WaveformComparator.similarity(model, learner)
                } else {
                    null
                }
            } else {
                null
            },
        )
    }

    /** Plays back the learner's own last recording (compare-by-ear). */
    fun playOwnRecording() {
        lastRecording?.let(courseAudioPlayer::playFile)
    }

    /** Plays the model recording for the current speaking activity. */
    fun playModelAudio(speed: Float = 1f) {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex)
        if (activity is com.spreva.core.model.LearningActivity.SpeakingRepeat) {
            courseAudioPlayer.playOrSpeak(
                contentPath = activity.audio,
                fallbackText = activity.text.de,
                speed = speed,
            )
        }
    }

    private fun _recordingDir(): java.io.File =
        java.io.File(appContext.cacheDir, "voice").also { it.mkdirs() }

    fun load(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = LessonUiState(isLoading = true)
            try {
                val lesson = getLesson(LessonId(lessonId))
                if (lesson == null) {
                    _uiState.value = LessonUiState(isLoading = false, error = "Lesson not found")
                    return@launch
                }
                learningRepository.ensureStarted(lesson.id, lesson.activities.size)
                attemptCounter.clear()
                currentActivityShownAtMs = clock.now().toEpochMilli()
                _uiState.value = LessonUiState(isLoading = false, lesson = lesson)
            } catch (t: Throwable) {
                _uiState.value = LessonUiState(isLoading = false, error = t.message ?: "Load failed")
            }
        }
    }

    fun onOptionSelected(optionId: String) {
        _uiState.value = _uiState.value.copy(selectedOptionId = optionId)
    }

    fun onAnswerChanged(answer: String) {
        _uiState.value = _uiState.value.copy(typedAnswer = answer)
    }

    /** Grades the current activity and records the attempt. */
    fun check() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val activity = lesson.activities.getOrNull(state.currentIndex) ?: return

        val answerState = when (activity) {
            is com.spreva.core.model.LearningActivity.MultipleChoice -> {
                val selected = state.selectedOptionId
                AnswerState(
                    correct = selected == activity.correctOptionId,
                    solution = activity.options
                        .firstOrNull { it.id == activity.correctOptionId }
                        ?.text?.de,
                )
            }

            is com.spreva.core.model.LearningActivity.Cloze -> {
                val normalized = normalize(state.typedAnswer)
                AnswerState(
                    correct = activity.acceptedAnswers.any { normalize(it) == normalized },
                    solution = activity.acceptedAnswers.firstOrNull(),
                )
            }

            is com.spreva.core.model.LearningActivity.ListeningChoice -> {
                val selected = state.selectedOptionId
                AnswerState(
                    correct = selected == activity.correctOptionId,
                    solution = activity.options
                        .firstOrNull { it.id == activity.correctOptionId }
                        ?.text?.de,
                )
            }

            is com.spreva.core.model.LearningActivity.Dictation -> {
                val normalized = normalize(state.typedAnswer)
                AnswerState(
                    correct = activity.acceptedAnswers.any { normalize(it) == normalized },
                    solution = activity.acceptedAnswers.firstOrNull(),
                )
            }

            is com.spreva.core.model.LearningActivity.FreeWrite -> {
                val count = wordCount(state.typedAnswer)
                AnswerState(
                    correct = count >= activity.minWords,
                    solution = if (count >= activity.minWords) null else "Minimum ${activity.minWords} words",
                )
            }

            is com.spreva.core.model.LearningActivity.SpeakingPrompt -> {
                val duration = state.recordingDurationMs ?: 0L
                val correct = state.hasRecording && duration >= activity.minSeconds * 1_000L
                AnswerState(
                    correct = correct,
                    solution = if (correct) null else "Record at least ${activity.minSeconds} seconds",
                )
            }

            else -> AnswerState(correct = true)
        }

        recordAttempt(
            lesson = lesson,
            activityId = activity.id,
            correct = answerState.correct,
        )

        val objective = activity is com.spreva.core.model.LearningActivity.MultipleChoice ||
            activity is com.spreva.core.model.LearningActivity.Cloze ||
            activity is com.spreva.core.model.LearningActivity.ListeningChoice ||
            activity is com.spreva.core.model.LearningActivity.Dictation
        val productive = activity is com.spreva.core.model.LearningActivity.FreeWrite ||
            activity is com.spreva.core.model.LearningActivity.SpeakingPrompt
        val rubric = if (productive && answerState.correct) {
            rubricFactory.forActivity(lesson.id, activity)
        } else {
            null
        }

        _uiState.value = state.copy(
            answerState = answerState,
            completedCount = if (answerState.correct) state.completedCount + 1 else state.completedCount,
            objectiveAttempted = state.objectiveAttempted + if (objective) 1 else 0,
            objectiveCorrect = state.objectiveCorrect + if (objective && answerState.correct) 1 else 0,
            productiveAttempted = state.productiveAttempted + if (productive) 1 else 0,
            productiveCompleted = state.productiveCompleted + if (productive && answerState.correct) 1 else 0,
            productionRubric = rubric,
            rubricRatings = emptyMap(),
            rubricSubmitted = false,
            rubricSaving = false,
            rubricScorePercent = null,
            rubricSaveError = null,
        )
    }

    fun onRubricRatingChanged(criterionId: String, rating: RubricRating) {
        val state = _uiState.value
        if (state.rubricSubmitted || state.productionRubric == null) return
        _uiState.value = state.copy(
            rubricRatings = state.rubricRatings + (criterionId to rating),
            rubricSaveError = null,
        )
    }

    fun submitProductionRubric() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val rubric = state.productionRubric ?: return
        if (!state.canSubmitRubric || state.rubricSubmitted) return
        val activity = lesson.activities.getOrNull(state.currentIndex) ?: return
        val score = runCatching { rubricScorer.score(rubric, state.rubricRatings) }
            .getOrElse {
                _uiState.value = state.copy(rubricSaveError = it.message ?: "Rubric incomplete")
                return
            }

        val assessment = ProductionSelfAssessment(
            lessonId = lesson.id,
            activityId = activity.id,
            level = rubric.level,
            mode = rubric.mode,
            ratings = state.rubricRatings,
            selfScorePercent = score,
            wordCount = (activity as? com.spreva.core.model.LearningActivity.FreeWrite)
                ?.let { wordCount(state.typedAnswer) },
            durationMs = (activity as? com.spreva.core.model.LearningActivity.SpeakingPrompt)
                ?.let { state.recordingDurationMs },
        )

        _uiState.value = state.copy(rubricSaving = true, rubricSaveError = null)
        viewModelScope.launch {
            runCatching {
                recordProductionSelfAssessment(assessment, clock.now())
            }.onSuccess {
                val latest = _uiState.value
                if (latest.lesson?.id == lesson.id &&
                    latest.lesson.activities.getOrNull(latest.currentIndex)?.id == activity.id
                ) {
                    _uiState.value = latest.copy(
                        rubricSubmitted = true,
                        rubricSaving = false,
                        rubricScorePercent = score,
                        rubricSaveError = null,
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    rubricSaving = false,
                    rubricSaveError = it.message ?: "Could not save self-review",
                )
            }
        }
    }

    /** Moves to the next activity (or stays on the summary). */
    fun next() {
        val state = _uiState.value
        val lesson = state.lesson ?: return
        val nextIndex = (state.currentIndex + 1).coerceAtMost(lesson.activities.lastIndex)
        currentActivityShownAtMs = clock.now().toEpochMilli()
        voiceRecorder.cancel()
        lastRecording?.delete()
        lastRecording = null
        _uiState.value = state.copy(
            currentIndex = nextIndex,
            selectedOptionId = null,
            typedAnswer = "",
            answerState = null,
            isRecording = false,
            hasRecording = false,
            similarityScore = null,
            recordingError = null,
            recordingDurationMs = null,
            productionRubric = null,
            rubricRatings = emptyMap(),
            rubricSubmitted = false,
            rubricScorePercent = null,
            rubricSaveError = null,
        )
    }

    /**
     * Marks the lesson complete: progress + event + review cards. The write
     * is tracked in [LessonUiState.completionState]; the UI navigates only
     * on [LessonCompletionState.Success] (audit §8 — an immediate navigation
     * used to tear down the ViewModel scope before the persistence landed).
     */
    fun finish() {
        val lesson = _uiState.value.lesson ?: return
        if (_uiState.value.completionState == LessonCompletionState.Saving) return
        _uiState.value = _uiState.value.copy(completionState = LessonCompletionState.Saving)
        viewModelScope.launch {
            runCatching {
                completeLesson(
                    lessonId = lesson.id,
                    totalActivities = lesson.activities.size,
                    now = clock.now(),
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(completionState = LessonCompletionState.Success)
            }.onFailure {
                _uiState.value = _uiState.value.copy(completionState = LessonCompletionState.Error)
            }
        }
    }

    private fun recordAttempt(lesson: Lesson, activityId: com.spreva.core.model.ActivityId, correct: Boolean) {
        val attemptNumber = (attemptCounter[activityId.value] ?: 0) + 1
        attemptCounter[activityId.value] = attemptNumber
        val now = clock.now()
        val attempt = ActivityAttempt(
            id = 0,
            lessonId = lesson.id,
            activityId = activityId,
            attemptNumber = attemptNumber,
            correct = correct,
            usedHint = false,
            responseTimeMs = (now.toEpochMilli() - currentActivityShownAtMs).coerceAtLeast(0),
            createdAt = now,
        )
        viewModelScope.launch {
            learningRepository.recordAttempt(attempt)
        }
    }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    private fun normalize(answer: String): String = answer
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
        .trimEnd('.', '!', '?', ',')
}
