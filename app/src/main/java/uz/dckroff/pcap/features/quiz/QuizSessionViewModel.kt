package uz.dckroff.pcap.features.quiz

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.data.model.*
import uz.dckroff.pcap.data.repository.QuizRepository
import java.util.*
import javax.inject.Inject

/**
 * ViewModel для экрана прохождения теста
 */
@HiltViewModel
class QuizSessionViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    // Текущий тест
    private val _quiz = MutableLiveData<Quiz>()
    val quiz: LiveData<Quiz> = _quiz

    // Текущий вопрос
    private val _currentQuestion = MutableLiveData<QuizQuestion>()
    val currentQuestion: LiveData<QuizQuestion> = _currentQuestion

    // Номер текущего вопроса (начиная с 1)
    private val _currentQuestionNumber = MutableLiveData<Int>()
    val currentQuestionNumber: LiveData<Int> = _currentQuestionNumber

    // Общее количество вопросов
    private val _totalQuestions = MutableLiveData<Int>()
    val totalQuestions: LiveData<Int> = _totalQuestions

    // Оставшееся время в миллисекундах
    private val _remainingTimeMillis = MutableLiveData<Long>()
    val remainingTimeMillis: LiveData<Long> = _remainingTimeMillis

    // Состояние загрузки
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Сообщение об ошибке
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Флаг завершения теста
    private val _testCompleted = MutableLiveData<Boolean>()
    val testCompleted: LiveData<Boolean> = _testCompleted

    // Прогресс прохождения теста (процент отвеченных вопросов)
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress

    // Результат теста
    private val _result = MutableLiveData<QuizResult>()
    val result: LiveData<QuizResult> = _result

    // Список вопросов теста
    private var questions: List<QuizQuestion> = emptyList()

    // Индекс текущего вопроса
    private var currentQuestionIndex = 0

    // Ответы пользователя (id вопроса -> список выбранных id вариантов)
    private val userAnswers = mutableMapOf<String, List<String>>()

    // Таймер для отсчета времени
    private var countDownTimer: CountDownTimer? = null

    // Время начала теста
    private var startTimeMillis: Long = 0

    /**
     * Загрузить тест по ID
     */
    fun loadQuiz(quizId: String) {
        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val quiz = quizRepository.getQuizById(quizId)
                
                if (quiz != null) {
                    _quiz.value = quiz
                    questions = quiz.questions
                    _totalQuestions.value = questions.size
                    _progress.value = 0
                    
                    // Переходим к первому вопросу
                    if (questions.isNotEmpty()) {
                        goToQuestion(0)
                    } else {
                        _error.value = "Тест не содержит вопросов"
                    }
                    
                    // Запускаем таймер
                    startTimer(quiz.timeLimit)
                } else {
                    _error.value = "Тест не найден"
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке теста $quizId")
                _error.value = "Ошибка при загрузке теста: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Перейти к определенному вопросу по индексу
     */
    private fun goToQuestion(index: Int) {
        if (index >= 0 && index < questions.size) {
            currentQuestionIndex = index
            _currentQuestion.value = questions[index]
            _currentQuestionNumber.value = index + 1  // +1 для отображения пользователю
            
            // Обновляем прогресс
            updateProgress()
        }
    }

    /**
     * Обновить прогресс прохождения теста
     */
    private fun updateProgress() {
        val answeredCount = userAnswers.size
        val totalCount = questions.size
        
        if (totalCount > 0) {
            val progress = (answeredCount * 100) / totalCount
            _progress.value = progress
        }
    }

    /**
     * Перейти к следующему вопросу
     */
    fun goToNextQuestion() {
        val nextIndex = currentQuestionIndex + 1
        if (nextIndex < questions.size) {
            goToQuestion(nextIndex)
        }
    }

    /**
     * Перейти к предыдущему вопросу
     */
    fun goToPreviousQuestion() {
        val prevIndex = currentQuestionIndex - 1
        if (prevIndex >= 0) {
            goToQuestion(prevIndex)
        }
    }

    /**
     * Сохранить ответ пользователя на текущий вопрос
     */
    fun saveAnswer(selectedOptionIds: List<String>) {
        val currentQuestion = _currentQuestion.value ?: return
        
        // Сохраняем ответ в локальном кэше
        userAnswers[currentQuestion.id] = selectedOptionIds
        
        // Обновляем прогресс
        updateProgress()
        
        // Добавляем логирование для отладки
        Timber.d("Сохранен ответ на вопрос ${currentQuestion.id}: $selectedOptionIds")
    }

    /**
     * Проверить, есть ли сохраненный ответ для текущего вопроса
     */
    fun getSavedAnswer(): List<String> {
        val currentQuestion = _currentQuestion.value ?: return emptyList()
        return userAnswers[currentQuestion.id] ?: emptyList()
    }

    /**
     * Завершить тест и подсчитать результаты
     */
    fun finishTest() {
        // Останавливаем таймер
        countDownTimer?.cancel()
        
        // Подсчитываем результаты
        calculateResults()
    }

    /**
     * Рассчитать результаты теста
     */
    private fun calculateResults() {
        val quiz = _quiz.value ?: return
        
        // Сохраняем результаты и ответы в одной корутине для синхронизации
        viewModelScope.launch {
            try {
                // Сначала сохраняем ответы пользователя
                val answersSaved = quizRepository.saveUserAnswers(quiz.id, userAnswers)
                if (answersSaved) {
                    Timber.d("Ответы пользователя сохранены успешно")
                } else {
                    Timber.e("Не удалось сохранить ответы пользователя")
                }
                
                // 2. Подсчитываем количество правильных ответов
                var correctAnswersCount = 0
                
                // Проверяем каждый ответ пользователя
                for (question in questions) {
                    val userAnswer = userAnswers[question.id] ?: continue
                    
                    // Для отладки
                    val correctOptions = question.options.filter { it.isCorrect }.map { it.id }
                    Timber.d("Вопрос: ${question.text}")
                    Timber.d("Правильные ответы: $correctOptions")
                    Timber.d("Ответы пользователя: $userAnswer")
                    
                    // Для вопросов с одним вариантом
                    if (question.type == QuestionType.SINGLE_CHOICE || question.type == QuestionType.TRUE_FALSE) {
                        val correctOption = question.options.find { it.isCorrect }
                        val isCorrect = correctOption != null && userAnswer.contains(correctOption.id)
                        
                        if (isCorrect) {
                            correctAnswersCount++
                            Timber.d("ВЕРНО! Правильный ответ на вопрос: ${question.text}")
                        } else {
                            Timber.d("НЕВЕРНО! Неправильный ответ на вопрос: ${question.text}")
                        }
                    } 
                    // Для вопросов с несколькими вариантами
                    else if (question.type == QuestionType.MULTIPLE_CHOICE) {
                        val correctOptionIds = question.options.filter { it.isCorrect }.map { it.id }
                        val incorrectOptionIds = question.options.filter { !it.isCorrect }.map { it.id }
                        
                        // Проверяем, что выбраны все правильные и не выбраны неправильные
                        val allCorrectSelected = correctOptionIds.all { userAnswer.contains(it) }
                        val noIncorrectSelected = incorrectOptionIds.none { userAnswer.contains(it) }
                        
                        if (allCorrectSelected && noIncorrectSelected) {
                            correctAnswersCount++
                            Timber.d("ВЕРНО! Правильный ответ на вопрос с множественным выбором: ${question.text}")
                        } else {
                            Timber.d("НЕВЕРНО! Неправильный ответ на вопрос с множественным выбором: ${question.text}")
                            Timber.d("Выбраны все правильные: $allCorrectSelected")
                            Timber.d("Нет неправильных: $noIncorrectSelected")
                        }
                    }
                }
                
                // 3. Рассчитываем процент правильных ответов
                val score = if (questions.isNotEmpty()) {
                    (correctAnswersCount * 100) / questions.size
                } else {
                    0
                }
                
                // Отладка
                Timber.d("ИТОГИ ТЕСТА:")
                Timber.d("Всего вопросов: ${questions.size}")
                Timber.d("Отвечено вопросов: ${userAnswers.size}")
                Timber.d("Правильных ответов: $correctAnswersCount")
                Timber.d("Процент правильных: $score%")
                
                // 4. Время прохождения в секундах
                val timeTakenSeconds = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
                
                // 5. Создаем объект с результатами
                val quizResult = QuizResult(
                    quizId = quiz.id,
                    score = score,
                    timeTaken = timeTakenSeconds,
                    completedAt = System.currentTimeMillis(),
                    answeredQuestions = userAnswers.size,
                    correctAnswers = correctAnswersCount
                )
                
                // 6. Сохраняем результат
                try {
                    val resultSaved = quizRepository.saveQuizResult(quizResult)
                    if (resultSaved) {
                        Timber.d("Результат теста сохранен успешно: $quizResult")
                    } else {
                        Timber.e("Не удалось сохранить результат теста")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при сохранении результата теста")
                } finally {
                    // 7. Отправляем результат в UI
                    _result.postValue(quizResult)
                    _testCompleted.postValue(true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при подсчете и сохранении результатов теста: ${e.message}")
                _testCompleted.postValue(true) // Все равно завершаем тест
            }
        }
    }

    /**
     * Запустить таймер обратного отсчета
     */
    private fun startTimer(timeMinutes: Int) {
        // Останавливаем предыдущий таймер, если он был
        countDownTimer?.cancel()
        
        // Запоминаем время начала теста
        startTimeMillis = System.currentTimeMillis()
        
        // Переводим минуты в миллисекунды
        val timeMillis = timeMinutes * 60 * 1000L
        
        countDownTimer = object : CountDownTimer(timeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTimeMillis.value = millisUntilFinished
            }

            override fun onFinish() {
                Timber.d("Время теста истекло")
                finishTest()
            }
        }.start()
    }

    /**
     * Получить общее количество отвеченных вопросов
     */
    fun getAnsweredQuestionsCount(): Int {
        return userAnswers.size
    }

    /**
     * Проверить, можно ли перейти к следующему вопросу
     */
    fun canGoToNextQuestion(): Boolean {
        return currentQuestionIndex < questions.size - 1
    }

    /**
     * Проверить, можно ли перейти к предыдущему вопросу
     */
    fun canGoToPreviousQuestion(): Boolean {
        return currentQuestionIndex > 0
    }

    /**
     * Проверить, заполнен ли текущий вопрос
     */
    fun isCurrentQuestionAnswered(): Boolean {
        val currentQuestion = _currentQuestion.value ?: return false
        return userAnswers.containsKey(currentQuestion.id)
    }

    /**
     * Проверить, все ли вопросы отвечены
     */
    fun areAllQuestionsAnswered(): Boolean {
        return userAnswers.size == questions.size
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
} 