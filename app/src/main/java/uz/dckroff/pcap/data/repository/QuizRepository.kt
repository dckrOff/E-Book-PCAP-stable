package uz.dckroff.pcap.data.repository

import timber.log.Timber
import uz.dckroff.pcap.data.model.*
import java.util.*
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
 * Реализация репозитория для работы с тестами (демо-данные)
 */
@Singleton
class QuizRepositoryImpl @Inject constructor() : QuizRepository {

    // Кэш для тестов
    private val quizzes = mutableListOf<Quiz>()
    
    // Кэш для результатов
    private val results = mutableListOf<QuizResult>()
    
    // Кэш для ответов пользователя (id теста -> (id вопроса -> список id ответов))
    private val userAnswers = mutableMapOf<String, Map<String, List<String>>>()

    /**
     * Получить список всех тестов
     */
    override suspend fun getAllQuizzes(): List<Quiz> {
        // Если кэш пуст, загружаем демо-данные
        if (quizzes.isEmpty()) {
            loadDummyQuizzes()
        }
        return quizzes
    }

    /**
     * Получить конкретный тест по ID
     */
    override suspend fun getQuizById(quizId: String): Quiz? {
        // Если кэш пуст, загружаем демо-данные
        if (quizzes.isEmpty()) {
            loadDummyQuizzes()
        }
        return quizzes.find { it.id == quizId }
    }

    /**
     * Сохранить результат прохождения теста
     */
    override suspend fun saveQuizResult(result: QuizResult): Boolean {
        return try {
            // Сохраняем результат
            results.add(result)
            
            // Обновляем статус теста и последний результат
            val quizIndex = quizzes.indexOfFirst { it.id == result.quizId }
            if (quizIndex != -1) {
                val quiz = quizzes[quizIndex]
                quizzes[quizIndex] = quiz.copy(
                    isCompleted = true,
                    lastScore = result.score
                )
            }
            
            Timber.d("Сохранен результат теста ${result.quizId}: ${result.score}%")
            true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении результата теста ${result.quizId}")
            false
        }
    }
    
    /**
     * Получить результат теста по ID
     */
    override suspend fun getQuizResult(quizId: String): QuizResult? {
        // Ищем результат в кэше
        val result = results.find { it.quizId == quizId }
        
        // Если результат найден, возвращаем его
        if (result != null) {
            Timber.d("Найден сохраненный результат теста $quizId: $result")
            return result
        }
        
        // Если результат не найден, но тест существует - создаем результат на основе ответов
        val quiz = quizzes.find { it.id == quizId }
        if (quiz != null) {
            // Получаем сохраненные ответы пользователя
            val answers = getUserAnswers(quizId)
            
            // Если есть ответы пользователя, создаем результат на их основе
            if (answers.isNotEmpty()) {
                Timber.d("Создаем результат на основе ответов пользователя для теста $quizId")
                
                // Подсчитываем количество правильных ответов
                var correctCount = 0
                quiz.questions.forEach { question ->
                    val userAnswer = answers[question.id] ?: emptyList()
                    val correctOptions = question.options.filter { it.isCorrect }.map { it.id }
                    
                    val isCorrect = when (question.type) {
                        QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE -> {
                            userAnswer.isNotEmpty() && correctOptions.contains(userAnswer[0])
                        }
                        QuestionType.MULTIPLE_CHOICE -> {
                            val correctSet = correctOptions.toSet()
                            val userSet = userAnswer.toSet()
                            correctSet == userSet
                        }
                    }
                    
                    if (isCorrect) {
                        correctCount++
                        Timber.d("Вопрос '${question.text}' отвечен верно")
                    } else {
                        Timber.d("Вопрос '${question.text}' отвечен неверно")
                        Timber.d("  Правильные ответы: $correctOptions")
                        Timber.d("  Ответы пользователя: $userAnswer")
                    }
                }
                
                // Рассчитываем процент
                val totalQuestions = quiz.questions.size
                val score = if (totalQuestions > 0) {
                    (correctCount * 100) / totalQuestions
                } else {
                    0
                }
                
                Timber.d("Итоги для теста $quizId:")
                Timber.d("  Всего вопросов: $totalQuestions")
                Timber.d("  Правильных ответов: $correctCount")
                Timber.d("  Процент правильных: $score%")
                
                // Создаем и возвращаем результат
                val quizResult = QuizResult(
                    quizId = quizId,
                    score = score,
                    timeTaken = 600, // 10 минут для демо
                    completedAt = System.currentTimeMillis() - 60000, // 1 минута назад
                    answeredQuestions = answers.size,
                    correctAnswers = correctCount
                )
                
                // Сохраняем результат в кэш для будущих запросов
                results.add(quizResult)
                
                return quizResult
            }
        }
        
        Timber.d("Результат теста $quizId не найден")
        return null
    }
    
    /**
     * Получить ответы пользователя для теста
     */
    override suspend fun getUserAnswers(quizId: String): Map<String, List<String>> {
        // Ищем ответы в кэше
        val answers = userAnswers[quizId]
        
        // Если не найдены, но тест существует - создаем демо-ответы
        if (answers == null) {
            val quiz = quizzes.find { it.id == quizId }
            if (quiz != null) {
                val demoAnswers = mutableMapOf<String, List<String>>()
                
                // Создаем ответы для каждого вопроса
                quiz.questions.forEachIndexed { index, question ->
                    // Для демонстрации: первые 2 вопроса отвечены правильно, остальные - случайно
                    if (index < 2) {
                        // Правильные ответы
                        val correctOptions = question.options.filter { it.isCorrect }.map { it.id }
                        demoAnswers[question.id] = correctOptions
                    } else {
                        // Неправильные или случайные ответы
                        val incorrectOptions = question.options.filter { !it.isCorrect }.map { it.id }
                        
                        when (question.type) {
                            QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE -> {
                                if (incorrectOptions.isNotEmpty()) {
                                    demoAnswers[question.id] = listOf(incorrectOptions.first())
                                }
                            }
                            QuestionType.MULTIPLE_CHOICE -> {
                                if (incorrectOptions.isNotEmpty()) {
                                    demoAnswers[question.id] = listOf(incorrectOptions.first())
                                }
                            }
                        }
                    }
                }
                
                // Сохраняем созданные ответы в кэш
                userAnswers[quizId] = demoAnswers
                return demoAnswers
            }
        }
        
        return answers ?: emptyMap()
    }

    /**
     * Загрузить демо-данные тестов
     */
    private fun loadDummyQuizzes() {
        quizzes.clear()
        quizzes.addAll(
            listOf(
                // Тест 1: Основы сетевых протоколов
                Quiz(
                    id = "quiz1",
                    title = "Основы сетевых протоколов",
                    description = "Проверьте свои знания по основам сетевых протоколов, включая модель OSI, TCP/IP и базовым концепциям.",
                    difficulty = QuizDifficulty.EASY,
                    questionsCount = 10,
                    timeLimit = 15,
                    isCompleted = false,
                    chapterIds = listOf("chapter1", "chapter2"),
                    questions = createNetworkBasicsQuestions()
                ),
                
                // Тест 2: Технология Ethernet
                Quiz(
                    id = "quiz2",
                    title = "Технология Ethernet",
                    description = "Углубленный тест по технологии Ethernet, включая стандарты, кадры и алгоритмы работы.",
                    difficulty = QuizDifficulty.MEDIUM,
                    questionsCount = 15,
                    timeLimit = 20,
                    isCompleted = false,
                    chapterIds = listOf("chapter3"),
                    questions = createEthernetQuestions()
                ),
                
                // Тест 3: Маршрутизация и коммутация
                Quiz(
                    id = "quiz3",
                    title = "Маршрутизация и коммутация",
                    description = "Сложный тест по принципам маршрутизации и коммутации в компьютерных сетях.",
                    difficulty = QuizDifficulty.HARD,
                    questionsCount = 20,
                    timeLimit = 30,
                    isCompleted = false,
                    chapterIds = listOf("chapter4", "chapter5"),
                    questions = createRoutingSwitchingQuestions()
                ),
                
                // Тест 4: Безопасность сетей
                Quiz(
                    id = "quiz4",
                    title = "Безопасность сетей",
                    description = "Проверка знаний основ сетевой безопасности, включая шифрование, брандмауэры и VPN.",
                    difficulty = QuizDifficulty.MEDIUM,
                    questionsCount = 15,
                    timeLimit = 25,
                    isCompleted = false,
                    chapterIds = listOf("chapter6"),
                    questions = createNetworkSecurityQuestions()
                ),
                
                // Тест 5: Анализ пакетов
                Quiz(
                    id = "quiz5",
                    title = "Анализ пакетов с Wireshark",
                    description = "Практический тест по анализу сетевых пакетов с использованием Wireshark.",
                    difficulty = QuizDifficulty.HARD,
                    questionsCount = 12,
                    timeLimit = 20,
                    isCompleted = false,
                    chapterIds = listOf("chapter7"),
                    questions = createPacketAnalysisQuestions()
                )
            )
        )
        
        Timber.d("Загружено ${quizzes.size} тестов")
    }
    
    /**
     * Создать вопросы для теста "Основы сетевых протоколов"
     */
    private fun createNetworkBasicsQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какой уровень модели OSI отвечает за маршрутизацию пакетов?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Физический уровень", false),
                    QuizOption(UUID.randomUUID().toString(), "Канальный уровень", false),
                    QuizOption(UUID.randomUUID().toString(), "Сетевой уровень", true),
                    QuizOption(UUID.randomUUID().toString(), "Транспортный уровень", false)
                ),
                explanation = "Сетевой уровень (Network Layer) отвечает за маршрутизацию пакетов между различными сетями."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какой протокол работает на транспортном уровне и обеспечивает надежную передачу данных?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "IP", false),
                    QuizOption(UUID.randomUUID().toString(), "TCP", true),
                    QuizOption(UUID.randomUUID().toString(), "HTTP", false),
                    QuizOption(UUID.randomUUID().toString(), "FTP", false)
                ),
                explanation = "TCP (Transmission Control Protocol) работает на транспортном уровне и обеспечивает надежную передачу данных с установлением соединения."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какие утверждения о протоколе UDP верны?",
                type = QuestionType.MULTIPLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "UDP устанавливает соединение перед передачей данных", false),
                    QuizOption(UUID.randomUUID().toString(), "UDP не гарантирует доставку пакетов", true),
                    QuizOption(UUID.randomUUID().toString(), "UDP быстрее TCP", true),
                    QuizOption(UUID.randomUUID().toString(), "UDP работает на сетевом уровне", false)
                ),
                explanation = "UDP не устанавливает соединение, не гарантирует доставку пакетов и их порядок, но работает быстрее TCP. UDP работает на транспортном уровне, а не на сетевом."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Протокол IP отвечает за:",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Установление соединения между узлами", false),
                    QuizOption(UUID.randomUUID().toString(), "Адресацию и маршрутизацию пакетов", true),
                    QuizOption(UUID.randomUUID().toString(), "Обеспечение надежной передачи данных", false),
                    QuizOption(UUID.randomUUID().toString(), "Шифрование данных", false)
                ),
                explanation = "Протокол IP (Internet Protocol) отвечает за адресацию и маршрутизацию пакетов в сети."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Верно ли, что протокол ICMP используется для передачи сообщений об ошибках в сети?",
                type = QuestionType.TRUE_FALSE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Верно", true),
                    QuizOption(UUID.randomUUID().toString(), "Неверно", false)
                ),
                explanation = "ICMP (Internet Control Message Protocol) используется для передачи сообщений об ошибках и управляющих сообщений в IP-сетях."
            )
        )
    }
    
    /**
     * Создать вопросы для теста "Технология Ethernet"
     */
    private fun createEthernetQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какой метод доступа к среде используется в Ethernet?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "CSMA/CD", true),
                    QuizOption(UUID.randomUUID().toString(), "CSMA/CA", false),
                    QuizOption(UUID.randomUUID().toString(), "Token Ring", false),
                    QuizOption(UUID.randomUUID().toString(), "FDDI", false)
                ),
                explanation = "В Ethernet используется метод доступа CSMA/CD (Carrier Sense Multiple Access with Collision Detection)."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какая максимальная скорость передачи данных в стандарте Gigabit Ethernet?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "100 Мбит/с", false),
                    QuizOption(UUID.randomUUID().toString(), "1 Гбит/с", true),
                    QuizOption(UUID.randomUUID().toString(), "10 Гбит/с", false),
                    QuizOption(UUID.randomUUID().toString(), "40 Гбит/с", false)
                ),
                explanation = "Gigabit Ethernet обеспечивает скорость передачи данных 1 Гбит/с (1000 Мбит/с)."
            )
        )
    }
    
    /**
     * Создать вопросы для теста "Маршрутизация и коммутация"
     */
    private fun createRoutingSwitchingQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какой протокол маршрутизации работает на основе алгоритма состояния связей (link-state)?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "RIP", false),
                    QuizOption(UUID.randomUUID().toString(), "OSPF", true),
                    QuizOption(UUID.randomUUID().toString(), "BGP", false),
                    QuizOption(UUID.randomUUID().toString(), "EIGRP", false)
                ),
                explanation = "OSPF (Open Shortest Path First) использует алгоритм состояния связей для вычисления оптимальных маршрутов."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какие утверждения о коммутаторах верны?",
                type = QuestionType.MULTIPLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Коммутаторы работают на канальном уровне модели OSI", true),
                    QuizOption(UUID.randomUUID().toString(), "Коммутаторы используют MAC-адреса для коммутации", true),
                    QuizOption(UUID.randomUUID().toString(), "Коммутаторы выполняют функции маршрутизации пакетов", false),
                    QuizOption(UUID.randomUUID().toString(), "Коммутаторы используют таблицу коммутации", true)
                ),
                explanation = "Коммутаторы работают на канальном уровне, используют MAC-адреса и таблицу коммутации для пересылки кадров. Маршрутизация пакетов - функция маршрутизаторов."
            )
        )
    }
    
    /**
     * Создать вопросы для теста "Безопасность сетей"
     */
    private fun createNetworkSecurityQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какой тип атаки направлен на переполнение системы множеством ложных запросов?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Атака типа Man-in-the-Middle", false),
                    QuizOption(UUID.randomUUID().toString(), "Фишинг", false),
                    QuizOption(UUID.randomUUID().toString(), "DDoS атака", true),
                    QuizOption(UUID.randomUUID().toString(), "Брутфорс", false)
                ),
                explanation = "DDoS (Distributed Denial of Service) атака направлена на переполнение системы множеством запросов с целью вызвать отказ в обслуживании."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Верно ли, что VPN шифрует весь сетевой трафик между клиентом и VPN-сервером?",
                type = QuestionType.TRUE_FALSE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Верно", true),
                    QuizOption(UUID.randomUUID().toString(), "Неверно", false)
                ),
                explanation = "VPN (Virtual Private Network) создает защищенный туннель, шифруя весь трафик между клиентом и VPN-сервером."
            )
        )
    }
    
    /**
     * Создать вопросы для теста "Анализ пакетов"
     */
    private fun createPacketAnalysisQuestions(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Какие из перечисленных фильтров можно использовать в Wireshark?",
                type = QuestionType.MULTIPLE_CHOICE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "ip.addr == 192.168.1.1", true),
                    QuizOption(UUID.randomUUID().toString(), "tcp.port == 443", true),
                    QuizOption(UUID.randomUUID().toString(), "http", true),
                    QuizOption(UUID.randomUUID().toString(), "packet.size > 1000", false)
                ),
                explanation = "В Wireshark можно использовать фильтры по IP-адресу, порту TCP и протоколу HTTP. Фильтр packet.size не является стандартным фильтром Wireshark."
            ),
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Верно ли, что Wireshark может декодировать зашифрованный SSL/TLS трафик?",
                type = QuestionType.TRUE_FALSE,
                options = listOf(
                    QuizOption(UUID.randomUUID().toString(), "Верно", false),
                    QuizOption(UUID.randomUUID().toString(), "Неверно", true)
                ),
                explanation = "Wireshark не может декодировать зашифрованный SSL/TLS трафик без доступа к ключам шифрования."
            )
        )
    }

    /**
     * Сохранить ответы пользователя
     */
    override suspend fun saveUserAnswers(quizId: String, answers: Map<String, List<String>>): Boolean {
        return try {
            // Сохраняем ответы пользователя
            userAnswers[quizId] = answers
            
            Timber.d("Сохранены ответы пользователя для теста ${quizId}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при сохранении ответов пользователя для теста ${quizId}")
            false
        }
    }
} 