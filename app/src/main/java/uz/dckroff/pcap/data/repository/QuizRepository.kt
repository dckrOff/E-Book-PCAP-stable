package uz.dckroff.pcap.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uz.dckroff.pcap.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Интерфейс репозитория для работы с тестами
 */
interface QuizRepository {
    /**
     * Получить список всех тестов
     */
    suspend fun getAllQuizzes(): List<Quiz>

    /**
     * Получить конкретный тест по ID
     */
    suspend fun getQuizById(quizId: String): Quiz?

    /**
     * Сохранить результат прохождения теста
     */
    suspend fun saveQuizResult(result: QuizResult): Boolean
    
    /**
     * Получить результат теста по ID
     */
    suspend fun getQuizResult(quizId: String): QuizResult?
    
    /**
     * Получить ответы пользователя для теста
     */
    suspend fun getUserAnswers(quizId: String): Map<String, List<String>>
    
    /**
     * Сохранить ответы пользователя
     */
    suspend fun saveUserAnswers(quizId: String, answers: Map<String, List<String>>): Boolean
}

/**
 * Реализация репозитория для работы с тестами из Firestore
 */
@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : QuizRepository {

    override suspend fun getAllQuizzes(): List<Quiz> {
        return try {
            val snapshot = firestore.collection("quizzes")
                .get()
                .await()
                
            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val topic = doc.getString("topic") ?: ""
                    val difficultyStr = doc.getString("difficulty") ?: "medium"
                    val difficulty = QuizDifficulty.fromString(difficultyStr)
                    val timeLimit = doc.getLong("timeLimit")?.toInt() ?: 30
                    
                    // Получаем вопросы
                    val questionsData = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                    val questions = questionsData.mapNotNull { questionMap ->
                        val questionId = questionMap["id"] as? String ?: return@mapNotNull null
                        val text = questionMap["text"] as? String ?: return@mapNotNull null
                        
                        // Получаем тип вопроса
                        val typeStr = questionMap["questionType"] as? String ?: "SINGLE_CHOICE"
                        val type = when (typeStr.uppercase()) {
                            "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE
                            "TRUE_FALSE" -> QuestionType.TRUE_FALSE
                            else -> QuestionType.SINGLE_CHOICE
                        }
                        
                        // Получаем варианты ответов (массив строк)
                        val optionsStrings = (questionMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        
                        // Получаем индексы правильных ответов
                        val correctOptionIndices = (questionMap["correctOptions"] as? List<*>)?.mapNotNull { 
                            when (it) {
                                is Long -> it.toInt()
                                is Int -> it
                                is Double -> it.toInt()
                                else -> null
                            }
                        } ?: listOf()
                        
                        val explanation = questionMap["explanation"] as? String
                        
                        // Преобразуем в объекты QuizOption
                        val options = optionsStrings.mapIndexed { index, text ->
                            QuizOption(
                                id = index.toString(),
                                text = text,
                                isCorrect = index in correctOptionIndices
                            )
                        }
                        
                        QuizQuestion(
                            id = questionId,
                            text = text,
                            options = options,
                            type = type,
                            explanation = explanation
                        )
                    }
                    
                    Quiz(
                        id = id,
                        title = title,
                        description = description,
                        topic = topic,
                        difficulty = difficulty,
                        timeLimit = timeLimit,
                        questions = questions
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing quiz document")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching quizzes from Firestore")
            emptyList()
        }
    }

    override suspend fun getQuizById(quizId: String): Quiz? {
        return try {
            val doc = firestore.collection("quizzes")
                .document(quizId)
                .get()
                .await()
                
            if (!doc.exists()) {
                return null
            }
            
            val title = doc.getString("title") ?: return null
            val description = doc.getString("description") ?: ""
            val topic = doc.getString("topic") ?: ""
            val difficultyStr = doc.getString("difficulty") ?: "medium"
            val difficulty = QuizDifficulty.fromString(difficultyStr)
            val timeLimit = doc.getLong("timeLimit")?.toInt() ?: 30
            
            // Получаем вопросы
            val questionsData = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
            val questions = questionsData.mapNotNull { questionMap ->
                val questionId = questionMap["id"] as? String ?: return@mapNotNull null
                val text = questionMap["text"] as? String ?: return@mapNotNull null
                
                // Получаем тип вопроса
                val typeStr = questionMap["questionType"] as? String ?: "SINGLE_CHOICE"
                val type = when (typeStr.uppercase()) {
                    "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE
                    "TRUE_FALSE" -> QuestionType.TRUE_FALSE
                    else -> QuestionType.SINGLE_CHOICE
                }
                
                // Получаем варианты ответов (массив строк)
                val optionsStrings = (questionMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // Получаем индексы правильных ответов
                val correctOptionIndices = (questionMap["correctOptions"] as? List<*>)?.mapNotNull { 
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        is Double -> it.toInt()
                        else -> null
                    }
                } ?: listOf()
                
                val explanation = questionMap["explanation"] as? String
                
                // Преобразуем в объекты QuizOption
                val options = optionsStrings.mapIndexed { index, text ->
                    QuizOption(
                        id = index.toString(),
                        text = text,
                        isCorrect = index in correctOptionIndices
                    )
                }
                
                QuizQuestion(
                    id = questionId,
                    text = text,
                    options = options,
                    type = type,
                    explanation = explanation
                )
            }
            
            Quiz(
                id = quizId,
                title = title,
                description = description,
                topic = topic,
                difficulty = difficulty,
                timeLimit = timeLimit,
                questions = questions
            )
        } catch (e: Exception) {
            Timber.e(e, "Error fetching quiz from Firestore")
            null
        }
    }

    override suspend fun saveQuizResult(result: QuizResult): Boolean {
        return try {
            firestore.collection("userProgress")
                .document("currentUser")
                .collection("quizResults")
                .document(result.quizId)
                .set(mapOf(
                    "quizId" to result.quizId,
                    "score" to result.score,
                    "timeTaken" to result.timeTaken,
                    "completedAt" to result.completedAt,
                    "answeredQuestions" to result.answeredQuestions,
                    "correctAnswers" to result.correctAnswers
                ))
                .await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving quiz result to Firestore")
            false
        }
    }
    
    override suspend fun getQuizResult(quizId: String): QuizResult? {
        return try {
            val doc = firestore.collection("userProgress")
                .document("currentUser")
                .collection("quizResults")
                .document(quizId)
                .get()
                .await()
                
            if (!doc.exists()) {
                return null
            }
            
            QuizResult(
                quizId = doc.getString("quizId") ?: quizId,
                score = doc.getLong("score")?.toInt() ?: 0,
                timeTaken = doc.getLong("timeTaken")?.toInt() ?: 0,
                completedAt = doc.getLong("completedAt") ?: 0,
                answeredQuestions = doc.getLong("answeredQuestions")?.toInt() ?: 0,
                correctAnswers = doc.getLong("correctAnswers")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Error fetching quiz result from Firestore")
            null
        }
    }

    override suspend fun getUserAnswers(quizId: String): Map<String, List<String>> {
        return try {
            val doc = firestore.collection("userProgress")
                .document("currentUser")
                .collection("quizAnswers")
                .document(quizId)
                .get()
                .await()
                
            if (!doc.exists()) {
                return emptyMap()
            }
            
            val answersMap = doc.data ?: return emptyMap()
            answersMap.mapValues { entry ->
                val value = entry.value
                when (value) {
                    is List<*> -> value.mapNotNull { it as? String }
                    is String -> listOf(value)
                    else -> emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching user answers from Firestore")
            emptyMap()
        }
    }

    override suspend fun saveUserAnswers(quizId: String, answers: Map<String, List<String>>): Boolean {
        return try {
            firestore.collection("userProgress")
                .document("currentUser")
                .collection("quizAnswers")
                .document(quizId)
                .set(answers)
                .await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving user answers to Firestore")
            false
        }
    }
} 