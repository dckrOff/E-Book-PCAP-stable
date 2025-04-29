package uz.dckroff.pcap.ui.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.Quiz
import uz.dckroff.pcap.data.model.QuizQuestion
import uz.dckroff.pcap.data.model.QuizResult
import uz.dckroff.pcap.data.repository.QuizRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import uz.dckroff.pcap.data.model.QuestionType

/**
 * ViewModel для экрана результатов теста
 */
@HiltViewModel
class QuizResultsViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    // Текущий тест
    private val _quiz = MutableLiveData<Quiz>()
    val quiz: LiveData<Quiz> = _quiz

    // Результат теста (если есть)
    private val _quizResult = MutableLiveData<QuizResult>()
    val quizResult: LiveData<QuizResult> = _quizResult

    // Список вопросов с ответами
    private val _questionsWithAnswers = MutableLiveData<List<QuestionWithAnswer>>()
    val questionsWithAnswers: LiveData<List<QuestionWithAnswer>> = _questionsWithAnswers

    // Состояние загрузки
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Сообщение об ошибке
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Форматированная дата прохождения
    private val _formattedDate = MutableLiveData<String>()
    val formattedDate: LiveData<String> = _formattedDate

    // Форматированное время прохождения
    private val _formattedTime = MutableLiveData<String>()
    val formattedTime: LiveData<String> = _formattedTime

    /**
     * Загрузить результаты теста по ID
     */
    fun loadQuizResults(quizId: String) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Загружаем тест
                val quiz = quizRepository.getQuizById(quizId)

                if (quiz != null) {
                    _quiz.value = quiz!!
                    Timber.d("Загружен тест: ${quiz.title}, завершен: ${quiz.isCompleted}")

                    // Загружаем последний результат
                    try {
                        // Получаем реальный результат теста
                        val result = quizRepository.getQuizResult(quizId)

                        if (result != null) {
                            _quizResult.value = result!!
                            Timber.d("Загружен результат теста: $result")
                            Timber.d("Процент правильных: ${result.score}%, правильных ответов: ${result.correctAnswers} из ${result.answeredQuestions}")

                            // Форматируем время и дату
                            formatCompletionDate(result.completedAt)
                            formatTimeTaken(result.timeTaken)

                            // Создаем список вопросов с ответами
                            loadUserAnswers(quiz.questions, quizId)
                        } else {
                            Timber.e("Результат теста не найден")
                            _error.value = "Результаты теста не найдены"
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при загрузке результатов теста: ${e.message}")
                        _error.value = "Ошибка при загрузке результатов: ${e.message}"
                    }
                } else {
                    _error.value = "Тест не найден"
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке теста $quizId")
                _error.value = "Ошибка при загрузке результатов: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Загрузить ответы пользователя для списка вопросов
     */
    private suspend fun loadUserAnswers(questions: List<QuizQuestion>, quizId: String) {
        try {
            // Получаем ответы пользователя из репозитория
            val userAnswers = quizRepository.getUserAnswers(quizId)
            Timber.d("Загружены ответы пользователя: $userAnswers")

            // Создаем список вопросов с ответами пользователя
            val questionsWithAnswers = questions.mapIndexed { index, question ->
                val userSelectedOptionIds = userAnswers[question.id] ?: emptyList()

                // Для отладки - проверяем правильность ответа
                val correctOptionIds = question.options.filter { it.isCorrect }.map { it.id }
                val isCorrect = when (question.type) {
                    QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE ->
                        userSelectedOptionIds.isNotEmpty() && correctOptionIds.contains(
                            userSelectedOptionIds[0]
                        )

                    QuestionType.MULTIPLE_CHOICE ->
                        userSelectedOptionIds.toSet() == correctOptionIds.toSet()

                    else -> false
                }

                Timber.d("Вопрос №${index + 1}: ${question.text}")
                Timber.d("  Правильные ответы: $correctOptionIds")
                Timber.d("  Ответы пользователя: $userSelectedOptionIds")
                Timber.d("  Ответ правильный: $isCorrect")

                QuestionWithAnswer(
                    question = question,
                    userSelectedOptionIds = userSelectedOptionIds,
                    number = index + 1
                )
            }

            _questionsWithAnswers.value = questionsWithAnswers
        } catch (e: Exception) {
            // Если не удалось загрузить ответы, создаем пустой список
            Timber.e(e, "Ошибка при загрузке ответов пользователя: ${e.message}")

            // В случае ошибки используем запасной вариант - результаты без ответов
            val questionsWithAnswers = questions.mapIndexed { index, question ->
                QuestionWithAnswer(
                    question = question,
                    userSelectedOptionIds = emptyList(),
                    number = index + 1
                )
            }

            _questionsWithAnswers.value = questionsWithAnswers
        }
    }

    /**
     * Форматировать дату прохождения теста
     */
    private fun formatCompletionDate(completedAt: Long) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        _formattedDate.value = dateFormat.format(Date(completedAt))
    }

    /**
     * Форматировать время прохождения теста
     */
    private fun formatTimeTaken(timeSeconds: Int) {
        val minutes = TimeUnit.SECONDS.toMinutes(timeSeconds.toLong())
        val seconds = timeSeconds % 60
        _formattedTime.value = String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Повторно пройти тест
     */
    fun retakeQuiz() {
        // Тут должна быть логика для повторного прохождения теста
        // В текущей реализации просто возвращаемся назад
    }

    /**
     * Модель данных для отображения вопроса с ответом
     */
    data class QuestionWithAnswer(
        val question: QuizQuestion,
        val userSelectedOptionIds: List<String>,
        val number: Int
    ) {
        /**
         * Проверить, верный ли ответ
         */
        fun isCorrect(): Boolean {
            val correctOptionIds = question.options.filter { it.isCorrect }.map { it.id }

            return when (question.type) {
                QuestionType.SINGLE_CHOICE,
                QuestionType.TRUE_FALSE -> {
                    correctOptionIds.isNotEmpty() &&
                            userSelectedOptionIds.isNotEmpty() &&
                            correctOptionIds.contains(userSelectedOptionIds[0])
                }

                QuestionType.MULTIPLE_CHOICE -> {
                    val correctSet = correctOptionIds.toSet()
                    val userSet = userSelectedOptionIds.toSet()
                    correctSet == userSet
                }
                // Обработка возможных будущих типов вопросов
                else -> false
            }
        }

        /**
         * Получить текст правильного ответа
         */
        fun getCorrectAnswerText(): String {
            val correctOptions = question.options.filter { it.isCorrect }
            return when (question.type) {
                QuestionType.SINGLE_CHOICE,
                QuestionType.TRUE_FALSE -> {
                    correctOptions.firstOrNull()?.text ?: ""
                }

                QuestionType.MULTIPLE_CHOICE -> {
                    correctOptions.joinToString("\n") { it.text }
                }

                else -> ""
            }
        }

        /**
         * Получить текст ответа пользователя
         */
        fun getUserAnswerText(): String {
            val userOptions = question.options.filter { userSelectedOptionIds.contains(it.id) }
            return when (question.type) {
                QuestionType.SINGLE_CHOICE,
                QuestionType.TRUE_FALSE -> {
                    userOptions.firstOrNull()?.text ?: ""
                }

                QuestionType.MULTIPLE_CHOICE -> {
                    userOptions.joinToString("\n") { it.text }
                }

                else -> ""
            }
        }
    }
} 