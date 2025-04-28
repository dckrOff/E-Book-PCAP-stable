package uz.dckroff.pcap.data.model

/**
 * Модель теста
 */
data class Quiz(
    val id: String,
    val title: String,
    val description: String,
    val topic: String,
    val difficulty: QuizDifficulty,
    val timeLimit: Int, // в минутах
    val isCompleted: Boolean = false,
    val lastScore: Int = 0, // процент правильных ответов, от 0 до 100
    val questions: List<QuizQuestion> = emptyList(),
    val chapterIds: List<String> = emptyList() // ID глав, к которым относится тест
)

/**
 * Сложность теста
 */
enum class QuizDifficulty {
    EASY,
    MEDIUM,
    HARD;
    
    fun getLocalizedName(): String {
        return when (this) {
            EASY -> "Легкий"
            MEDIUM -> "Средний"
            HARD -> "Сложный"
        }
    }
    
    fun getColorRes(): Int {
        return when (this) {
            EASY -> android.R.color.holo_green_light
            MEDIUM -> android.R.color.holo_orange_light
            HARD -> android.R.color.holo_red_light
        }
    }

    companion object {
        fun fromString(value: String): QuizDifficulty {
            return when (value.lowercase()) {
                "easy" -> EASY
                "medium" -> MEDIUM
                "hard" -> HARD
                else -> MEDIUM
            }
        }
    }
}

/**
 * Тип вопроса
 */
enum class QuestionType {
    SINGLE_CHOICE, // выбор одного варианта
    MULTIPLE_CHOICE, // выбор нескольких вариантов
    TRUE_FALSE // правда/ложь
}

/**
 * Модель вопроса теста
 */
data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<QuizOption>,
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val explanation: String? = null // объяснение правильного ответа
)

/**
 * Модель варианта ответа
 */
data class QuizOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean
)

/**
 * Результат прохождения теста
 */
data class QuizResult(
    val quizId: String,
    val score: Int, // процент правильных ответов, от 0 до 100
    val timeTaken: Int, // в секундах
    val completedAt: Long, // время завершения в миллисекундах
    val answeredQuestions: Int, // количество отвеченных вопросов
    val correctAnswers: Int // количество правильных ответов
) 