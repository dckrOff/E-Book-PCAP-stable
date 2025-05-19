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
 * ViewModel для экрана результатов теста с улучшенной обработкой ошибок
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

    // Флаг успешной загрузки данных
    private val _dataLoaded = MutableLiveData<Boolean>(false)
    val dataLoaded: LiveData<Boolean> = _dataLoaded

    /**
     * Загрузить результаты теста по ID
     */
    fun loadQuizResults(quizId: String) {
        if (quizId.isEmpty()) {
            _error.value = "ID теста не указан"
            return
        }

        _loading.value = true
        _error.value = null
        _dataLoaded.value = false

        viewModelScope.launch {
            try {
                // Загружаем тест
                val quizResult = loadQuiz(quizId)
                if (quizResult) {
                    // Загружаем результаты, только если тест был успешно загружен
                    loadResults(quizId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Необработанная ошибка при загрузке результатов теста: ${e.message}")
                _error.value = "Ошибка при загрузке результатов: ${e.message ?: "Неизвестная ошибка"}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Загрузить тест по ID
     */
    private suspend fun loadQuiz(quizId: String): Boolean {
        return try {
            val quiz = quizRepository.getQuizById(quizId)

            if (quiz != null) {
                _quiz.postValue(quiz!!)
                Timber.d("Загружен тест: ${quiz.title}, завершен: ${quiz.isCompleted}")
                true
            } else {
                Timber.e("Тест с ID $quizId не найден")
                _error.postValue("Тест не найден")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при загрузке теста $quizId: ${e.message}")
            _error.postValue("Ошибка при загрузке теста: ${e.message ?: "Неизвестная ошибка"}")
            false
        }
    }

    /**
     * Загрузить результаты теста
     */
    private suspend fun loadResults(quizId: String) {
        try {
            // Получаем результат теста
            val result = quizRepository.getQuizResult(quizId)

            if (result != null) {
                _quizResult.postValue(result!!)
                Timber.d("Загружен результат теста: $result")
                Timber.d("Процент правильных: ${result.score}%, правильных ответов: ${result.correctAnswers} из ${result.answeredQuestions}")

                // Форматируем время и дату
                formatCompletionDate(result.completedAt)
                formatTimeTaken(result.timeTaken)

                // Загружаем вопросы с ответами пользователя
                val quiz = _quiz.value
                if (quiz != null) {
                    loadUserAnswers(quiz.questions, quizId)
                    _dataLoaded.postValue(true)
                } else {
                    Timber.e("Невозможно загрузить ответы: тест равен null")
                    _error.postValue("Ошибка при загрузке ответов: тест не найден")
                }
            } else {
                Timber.e("Результат теста не найден для ID $quizId")
                _error.postValue("Результаты теста не найдены")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при загрузке результатов теста: ${e.message}")
            _error.postValue("Ошибка при загрузке результатов: ${e.message ?: "Неизвестная ошибка"}")
        }
    }

    /**
     * Загрузить ответы пользователя для списка вопросов
     */
    private suspend fun loadUserAnswers(questions: List<QuizQuestion>, quizId: String) {
        try {
            if (questions.isEmpty()) {
                Timber.w("Список вопросов пуст")
                _questionsWithAnswers.postValue(emptyList())
                return
            }

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

            _questionsWithAnswers.postValue(questionsWithAnswers)
        } catch (e: Exception) {
            // Если не удалось загрузить ответы, создаем список с пустыми ответами
            Timber.e(e, "Ошибка при загрузке ответов пользователя: ${e.message}")

            val questionsWithAnswers = questions.mapIndexed { index, question ->
                QuestionWithAnswer(
                    question = question,
                    userSelectedOptionIds = emptyList(),
                    number = index + 1
                )
            }

            _questionsWithAnswers.postValue(questionsWithAnswers)
            // Не показываем ошибку пользователю, так как это не критичная ошибка
            Timber.w("Используем резервный вариант с пустыми ответами")
        }
    }

    /**
     * Форматировать дату прохождения теста
     */
    private fun formatCompletionDate(completedAt: Long) {
        try {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            _formattedDate.postValue(dateFormat.format(Date(completedAt)))
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при форматировании даты: ${e.message}")
            _formattedDate.postValue("Нет данных")
        }
    }

    /**
     * Форматировать время прохождения теста
     */
    private fun formatTimeTaken(timeSeconds: Int) {
        try {
            val minutes = TimeUnit.SECONDS.toMinutes(timeSeconds.toLong())
            val seconds = timeSeconds % 60
            _formattedTime.postValue(String.format("%02d:%02d", minutes, seconds))
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при форматировании времени: ${e.message}")
            _formattedTime.postValue("--:--")
        }
    }

    /**
     * Очистить данные и сбросить состояние
     */
    fun resetState() {
        _loading.value = false
        _error.value = null
        _dataLoaded.value = false
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
                    correctOptions.firstOrNull()?.text ?: "Нет правильного ответа"
                }

                QuestionType.MULTIPLE_CHOICE -> {
                    if (correctOptions.isEmpty()) {
                        "Нет правильных ответов"
                    } else {
                        correctOptions.joinToString("\n") { it.text }
                    }
                }

                else -> "Неизвестный тип вопроса"
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
                    userOptions.firstOrNull()?.text ?: "Ответ не выбран"
                }

                QuestionType.MULTIPLE_CHOICE -> {
                    if (userOptions.isEmpty()) {
                        "Ответы не выбраны"
                    } else {
                        userOptions.joinToString("\n") { it.text }
                    }
                }

                else -> "Неизвестный тип вопроса"
            }
        }
    }
}