package uz.dckroff.pcap.features.quiz

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
        
        var quizId = ""
        try {
            quizId = args.quizId
        } catch (e: Exception) {
            // Если args не сработал, попробуем получить аргумент из Bundle
            quizId = arguments?.getString("quizId") ?: ""
            Timber.e("Error getting args.quizId, using Bundle instead: $e")
        }
        
        Timber.d("QuizResultsFragment created with quizId: $quizId")
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupBackHandling()
        
        // Загружаем результаты теста
        if (quizId.isNotEmpty()) {
            viewModel.loadQuizResults(quizId)
        } else {
            // Обработка ошибки - ID теста не указан
            Timber.e("No quizId provided")
            binding.errorView.root.visibility = View.VISIBLE
            binding.errorView.textErrorMessage.text = "Ошибка: ID теста не указан"
        }
    }
    
    private fun setupBackHandling() {
        // Обработка системной кнопки "Назад"
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
     * Настройка верхней панели инструментов
     */
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.quiz_results_title)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }
    
    private fun setupRecyclerView() {
        adapter = QuestionResultAdapter()
        binding.questionsRecyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        // Настраиваем кнопку "Назад"
        binding.toolbar.setNavigationOnClickListener {
            navigateToQuizList()
        }
        
        // Настраиваем кнопку "Пройти заново"
        binding.retakeQuizButton.setOnClickListener {
            val quiz = viewModel.quiz.value ?: return@setOnClickListener
            
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
    }
    
    private fun navigateToQuizList() {
        Timber.d("Navigating back to QuizListFragment")
        
        // Попробуем несколько способов вернуться к списку тестов
        try {
            // Способ 1: Использовать MainActivity и активировать соответствующую вкладку
            (requireActivity() as? MainActivity)?.let { activity ->
                activity.navigateToMainSection(2) // Индекс вкладки с тестами
                return
            }
            
            // Способ 2: Попробовать вернуться к конкретному фрагменту
            findNavController().popBackStack(R.id.quizListFragment, false)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при навигации к списку тестов")
            
            // Запасной вариант: просто вернуться назад
            findNavController().popBackStack()
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за тестом
        viewModel.quiz.observe(viewLifecycleOwner) { quiz ->
            binding.quizTitleTextView.text = quiz.title
        }
        
        // Наблюдаем за датой прохождения
        viewModel.formattedDate.observe(viewLifecycleOwner) { formattedDate ->
            binding.attemptDateTextView.text = getString(R.string.attempt_date, formattedDate)
        }
        
        // Наблюдаем за результатом теста
        viewModel.quizResult.observe(viewLifecycleOwner) { result ->
            // Отображаем результат
            binding.scoreTextView.text = "${result.score}%"
            
            // Отображаем количество правильных ответов
            binding.correctAnswersTextView.text = getString(
                R.string.correct_answers_count,
                result.correctAnswers,
                result.answeredQuestions
            )
        }
        
        // Наблюдаем за форматированным временем прохождения
        viewModel.formattedTime.observe(viewLifecycleOwner) { formattedTime ->
            binding.timeSpentTextView.text = getString(R.string.time_spent, formattedTime)
        }
        
        // Наблюдаем за списком вопросов с ответами
        viewModel.questionsWithAnswers.observe(viewLifecycleOwner) { questions ->
            adapter.submitList(questions)
            
            // Показываем заголовок "Обзор ответов", если есть вопросы
            binding.questionsHeaderTextView.visibility = if (questions.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        // Наблюдаем за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            // Скрываем/показываем контент при загрузке
            val contentVisibility = if (isLoading) View.GONE else View.VISIBLE
            binding.scoreCard.visibility = contentVisibility
            binding.questionsHeaderTextView.visibility = contentVisibility
            binding.questionsRecyclerView.visibility = contentVisibility
            binding.retakeQuizButton.visibility = contentVisibility
        }
        
        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.errorView.root.visibility = View.VISIBLE
                binding.errorView.textErrorMessage.text = errorMessage
                
                // Скрываем контент при ошибке
                binding.scoreCard.visibility = View.GONE
                binding.questionsHeaderTextView.visibility = View.GONE
                binding.questionsRecyclerView.visibility = View.GONE
                binding.retakeQuizButton.visibility = View.GONE
            } else {
                binding.errorView.root.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 