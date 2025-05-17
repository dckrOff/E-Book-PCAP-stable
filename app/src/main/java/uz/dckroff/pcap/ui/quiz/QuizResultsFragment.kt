package uz.dckroff.pcap.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.MainActivity
import uz.dckroff.pcap.R
import uz.dckroff.pcap.databinding.FragmentQuizResultsBinding

@AndroidEntryPoint
class QuizResultsFragment : Fragment() {

    private var _binding: FragmentQuizResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizResultsViewModel by viewModels()
    private val args: QuizResultsFragmentArgs by navArgs()
    private lateinit var adapter: QuestionResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем адаптер до установки слушателей и наблюдателей
        setupRecyclerView()

        // Сначала показываем загрузку и скрываем контент
        showLoading(true)

        // Получаем ID теста из безопасных источников
        val quizId = getQuizId()

        setupToolbar()
        setupListeners()
        setupObservers()
        setupBackHandling()

        // Загружаем результаты теста только если ID получен
        if (quizId.isNotEmpty()) {
            viewModel.loadQuizResults(quizId)
        } else {
            // Обработка ошибки - ID теста не указан
            Timber.e("No quizId provided")
            showError(getString(R.string.error_no_quiz_id))
        }
    }

    /**
     * Безопасное получение ID теста из разных источников
     */
    private fun getQuizId(): String {
        var quizId = ""

        try {
            // Попытка 1: Получить из safeArgs
            quizId = args.quizId
            Timber.d("QuizId received from SafeArgs: $quizId")
        } catch (e: Exception) {
            Timber.e("Failed to get quizId from SafeArgs: ${e.message}")

            try {
                // Попытка 2: Получить из Bundle
                quizId = arguments?.getString("quizId") ?: ""
                Timber.d("QuizId received from Bundle: $quizId")
            } catch (e: Exception) {
                Timber.e("Failed to get quizId from Bundle: ${e.message}")
            }
        }

        Timber.d("Final quizId: $quizId")
        return quizId
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToQuizList()
                }
            }
        )
    }

    /**
     * Показывает или скрывает индикатор загрузки и контент
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Скрываем/показываем контент при загрузке
        val contentVisibility = if (isLoading) View.GONE else View.VISIBLE
        binding.scoreCard.visibility = contentVisibility
        binding.questionsHeaderTextView.visibility = contentVisibility
        binding.questionsRecyclerView.visibility = contentVisibility
        binding.retakeQuizButton.visibility = contentVisibility

        // Скрываем ошибку при загрузке
        binding.errorView.root.visibility = View.GONE
    }

    /**
     * Показывает сообщение об ошибке и скрывает контент
     */
    private fun showError(errorMessage: String) {
        Timber.e("Showing error: $errorMessage")

        // Показываем ошибку
        binding.errorView.root.visibility = View.VISIBLE
        binding.errorView.textErrorMessage.text = errorMessage

        // Скрываем индикатор загрузки и контент
        binding.progressBar.visibility = View.GONE
        binding.scoreCard.visibility = View.GONE
        binding.questionsHeaderTextView.visibility = View.GONE
        binding.questionsRecyclerView.visibility = View.GONE
        binding.retakeQuizButton.visibility = View.GONE
    }

    /**
     * Показывает контент и скрывает ошибку и загрузку
     */
    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.errorView.root.visibility = View.GONE

        binding.scoreCard.visibility = View.VISIBLE
        binding.retakeQuizButton.visibility = View.VISIBLE

        // Показываем список вопросов только если он не пустой
        val hasQuestions = adapter.itemCount > 0
        binding.questionsHeaderTextView.visibility = if (hasQuestions) View.VISIBLE else View.GONE
        binding.questionsRecyclerView.visibility = if (hasQuestions) View.VISIBLE else View.GONE
    }

    /**
     * Настройка верхней панели инструментов
     */
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.quiz_results_title)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            navigateToQuizList()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuestionResultAdapter()
        binding.questionsRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        // Настраиваем кнопку "Пройти заново"
        binding.retakeQuizButton.setOnClickListener {
            val quiz = viewModel.quiz.value
            if (quiz == null) {
                Timber.e("Cannot retake quiz: quiz is null")
                showError(getString(R.string.error_quiz_not_available))
                return@setOnClickListener
            }

            // Создаем бандл с ID теста
            val bundle = Bundle().apply {
                putString("quizId", quiz.id)
            }

            // Переходим к экрану прохождения теста
            findNavController().navigate(
                R.id.quizSessionFragment,
                bundle
            )
        }

        // Настраиваем кнопку повтора при ошибке (если такая есть в макете)
        binding.errorView.buttonRetry?.setOnClickListener {
            val quizId = getQuizId()
            if (quizId.isNotEmpty()) {
                showLoading(true)
                viewModel.loadQuizResults(quizId)
            } else {
                showError(getString(R.string.error_no_quiz_id))
            }
        }
    }

    private fun navigateToQuizList() {
        Timber.d("Navigating back to QuizListFragment")

        try {
            // Способ 1: Использовать MainActivity и активировать соответствующую вкладку
            (requireActivity() as? MainActivity)?.let { activity ->
                activity.navigateToMainSection(2) // Индекс вкладки с тестами
                return
            }

            // Способ 2: Попробовать вернуться к конкретному фрагменту
            findNavController().popBackStack(R.id.quizListFragment, false)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при навигации к списку тестов: ${e.message}")

            // Запасной вариант: просто вернуться назад
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        // Наблюдаем за тестом
        viewModel.quiz.observe(viewLifecycleOwner) { quiz ->
            if (quiz != null) {
                binding.quizTitleTextView.text = quiz.title
            } else {
                Timber.e("Received null quiz")
                showError(getString(R.string.error_quiz_not_available))
            }
        }

        // Наблюдаем за датой прохождения
        viewModel.formattedDate.observe(viewLifecycleOwner) { formattedDate ->
            binding.attemptDateTextView.text = getString(R.string.attempt_date, formattedDate)
        }

        // Наблюдаем за результатом теста
        viewModel.quizResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                // Отображаем результат
                binding.scoreTextView.text = "${result.score}%"

                // Отображаем количество правильных ответов
                binding.correctAnswersTextView.text = getString(
                    R.string.correct_answers_count,
                    result.correctAnswers,
                    result.answeredQuestions
                )
            } else {
                Timber.e("Received null quiz result")
                showError(getString(R.string.error_loading_quiz))
            }
        }

        // Наблюдаем за форматированным временем прохождения
        viewModel.formattedTime.observe(viewLifecycleOwner) { formattedTime ->
            binding.timeSpentTextView.text = getString(R.string.time_spent, formattedTime)
        }

        // Наблюдаем за списком вопросов с ответами
        viewModel.questionsWithAnswers.observe(viewLifecycleOwner) { questions ->
            if (questions != null) {
                adapter.submitList(questions)

                // Обновляем видимость заголовка вопросов
                binding.questionsHeaderTextView.visibility =
                    if (questions.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Наблюдаем за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null && errorMessage.isNotEmpty()) {
                showError(errorMessage)
            } else {
                // Если ошибок нет и загрузка завершена, показываем контент
                if (viewModel.loading.value == false) {
                    showContent()
                }
            }
        }

        // Наблюдаем за успешной загрузкой данных
        viewModel.dataLoaded.observe(viewLifecycleOwner) { isLoaded ->
            if (isLoaded) {
                showContent()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}