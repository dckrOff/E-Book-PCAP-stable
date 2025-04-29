package uz.dckroff.pcap.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.QuestionType
import uz.dckroff.pcap.data.model.QuizQuestion
import uz.dckroff.pcap.databinding.FragmentQuizSessionBinding
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class QuizSessionFragment : Fragment() {

    private var _binding: FragmentQuizSessionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: QuizSessionViewModel by viewModels()
    private val args: QuizSessionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizSessionBinding.inflate(inflater, container, false)
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
        
        Timber.d("QuizSessionFragment created with quizId: $quizId")
        
        setupBackPressHandler()
        setupListeners()
        setupObservers()
        
        // Загружаем тест
        if (quizId.isNotEmpty()) {
            viewModel.loadQuiz(quizId)
        } else {
            // Обработка ошибки - ID теста не указан
            Timber.e("No quizId provided")
            binding.errorView.root.visibility = View.VISIBLE
            binding.errorView.textErrorMessage.text = "Ошибка: ID теста не указан"
        }
    }
    
    private fun setupBackPressHandler() {
        // Обрабатываем нажатие кнопки "Назад"
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        
        // Также обрабатываем нажатие на стрелку "Назад" в тулбаре
        binding.toolbar.setNavigationOnClickListener {
            showExitConfirmationDialog()
        }
    }
    
    private fun setupListeners() {
        // Кнопка "Предыдущий вопрос"
        binding.previousButton.setOnClickListener {
            saveCurrentAnswer()
            viewModel.goToPreviousQuestion()
        }
        
        // Кнопка "Следующий вопрос"
        binding.nextButton.setOnClickListener {
            saveCurrentAnswer()
            viewModel.goToNextQuestion()
        }
        
        // Кнопка "Завершить тест"
        binding.finishButton.setOnClickListener {
            saveCurrentAnswer()
            showFinishConfirmationDialog()
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за текущим тестом
        viewModel.quiz.observe(viewLifecycleOwner) { quiz ->
            binding.quizTitleTextView.text = quiz.title
        }
        
        // Наблюдаем за текущим вопросом
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            displayQuestion(question)
        }
        
        // Наблюдаем за номером текущего вопроса
        viewModel.currentQuestionNumber.observe(viewLifecycleOwner) { number ->
            val total = viewModel.totalQuestions.value ?: 0
            binding.questionNumberTextView.text = "Вопрос $number/$total"
            
            // Обновляем состояние кнопок навигации
            binding.previousButton.isEnabled = viewModel.canGoToPreviousQuestion()
            binding.nextButton.isEnabled = viewModel.canGoToNextQuestion()
        }
        
        // Наблюдаем за прогрессом
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.progressIndicator.progress = progress
            
            val answeredCount = viewModel.getAnsweredQuestionsCount()
            val totalCount = viewModel.totalQuestions.value ?: 0
            binding.progressTextView.text = "$answeredCount/$totalCount вопросов отвечено"
            
            // Показываем кнопку "Завершить", если все вопросы отвечены
            binding.finishButton.isEnabled = viewModel.areAllQuestionsAnswered()
        }
        
        // Наблюдаем за оставшимся временем
        viewModel.remainingTimeMillis.observe(viewLifecycleOwner) { timeMillis ->
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) % 60
            binding.timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        }
        
        // Наблюдаем за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.questionCard.visibility = if (!isLoading) View.VISIBLE else View.GONE
            binding.navigationButtonsLayout.visibility = if (!isLoading) View.VISIBLE else View.GONE
            binding.finishButton.visibility = if (!isLoading) View.VISIBLE else View.GONE
        }
        
        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.errorView.root.visibility = View.VISIBLE
                binding.errorView.textErrorMessage.text = errorMessage
                binding.questionCard.visibility = View.GONE
                binding.navigationButtonsLayout.visibility = View.GONE
                binding.finishButton.visibility = View.GONE
            } else {
                binding.errorView.root.visibility = View.GONE
            }
        }
        
        // Наблюдаем за завершением теста
        viewModel.testCompleted.observe(viewLifecycleOwner) { isCompleted ->
            if (isCompleted) {
                navigateToResults()
            }
        }
    }
    
    /**
     * Отобразить текущий вопрос
     */
    private fun displayQuestion(question: QuizQuestion) {
        // Устанавливаем текст вопроса
        binding.questionTextView.text = question.text
        
        // Скрываем все элементы ввода
        binding.singleChoiceGroup.visibility = View.GONE
        binding.multipleChoiceGroup.visibility = View.GONE
        binding.trueFalseGroup.visibility = View.GONE
        binding.textAnswerLayout.visibility = View.GONE
        
        // Очищаем предыдущие элементы
        binding.singleChoiceGroup.removeAllViews()
        binding.multipleChoiceGroup.removeAllViews()
        
        // Отображаем элементы в зависимости от типа вопроса
        when (question.type) {
            QuestionType.SINGLE_CHOICE -> {
                displaySingleChoiceQuestion(question)
            }
            QuestionType.MULTIPLE_CHOICE -> {
                displayMultipleChoiceQuestion(question)
            }
            QuestionType.TRUE_FALSE -> {
                displayTrueFalseQuestion(question)
            }
        }
    }
    
    /**
     * Отобразить вопрос с одним вариантом ответа
     */
    private fun displaySingleChoiceQuestion(question: QuizQuestion) {
        binding.singleChoiceGroup.visibility = View.VISIBLE
        
        // Очищаем все предыдущие радио-кнопки
        binding.singleChoiceGroup.removeAllViews()
        
        // Получаем сохраненный ответ, если есть
        val savedAnswer = viewModel.getSavedAnswer()
        
        // Создаем радио-кнопки для каждого варианта
        for (option in question.options) {
            val radioButton = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = option.text
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                // Устанавливаем выбранный вариант, если ответ был сохранен ранее
                isChecked = savedAnswer.contains(option.id)
                
                // Добавляем обработчик изменения состояния
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Сохраняем ответ и обновляем состояние кнопки
                        viewModel.saveAnswer(listOf(option.id))
                        updateFinishButtonState()
                    }
                }
            }
            
            binding.singleChoiceGroup.addView(radioButton)
        }
    }
    
    /**
     * Отобразить вопрос с несколькими вариантами ответа
     */
    private fun displayMultipleChoiceQuestion(question: QuizQuestion) {
        binding.multipleChoiceGroup.visibility = View.VISIBLE
        
        // Очищаем все предыдущие чекбоксы
        binding.multipleChoiceGroup.removeAllViews()
        
        // Получаем сохраненный ответ, если есть
        val savedAnswer = viewModel.getSavedAnswer()
        
        // Создаем чекбоксы для каждого варианта
        for (option in question.options) {
            val checkBox = CheckBox(requireContext()).apply {
                id = View.generateViewId()
                text = option.text
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                // Устанавливаем выбранный вариант, если ответ был сохранен ранее
                isChecked = savedAnswer.contains(option.id)
                
                // Добавляем обработчик изменения состояния
                setOnCheckedChangeListener { _, _ ->
                    // Собираем все выбранные варианты и сохраняем
                    val selectedIds = mutableListOf<String>()
                    for (i in 0 until binding.multipleChoiceGroup.childCount) {
                        val cb = binding.multipleChoiceGroup.getChildAt(i) as? CheckBox
                        if (cb != null && cb.isChecked && i < question.options.size) {
                            selectedIds.add(question.options[i].id)
                        }
                    }
                    if (selectedIds.isNotEmpty()) {
                        viewModel.saveAnswer(selectedIds)
                        updateFinishButtonState()
                    }
                }
            }
            
            binding.multipleChoiceGroup.addView(checkBox)
        }
    }
    
    /**
     * Отобразить вопрос типа "Правда/Ложь"
     */
    private fun displayTrueFalseQuestion(question: QuizQuestion) {
        binding.trueFalseGroup.visibility = View.VISIBLE
        
        // Получаем сохраненный ответ, если есть
        val savedAnswer = viewModel.getSavedAnswer()
        
        // Устанавливаем выбранный вариант, если ответ был сохранен ранее
        if (savedAnswer.isNotEmpty()) {
            val selectedOptionId = savedAnswer.first()
            val trueOption = question.options.find { it.text == "Верно" }
            
            if (trueOption != null) {
                binding.trueRadioButton.isChecked = selectedOptionId == trueOption.id
                binding.falseRadioButton.isChecked = selectedOptionId != trueOption.id
            }
        } else {
            binding.trueRadioButton.isChecked = false
            binding.falseRadioButton.isChecked = false
        }
        
        // Добавляем обработчики изменения состояния
        binding.trueRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val trueOption = question.options.find { it.text == "Верно" }
                if (trueOption != null) {
                    viewModel.saveAnswer(listOf(trueOption.id))
                    updateFinishButtonState()
                }
            }
        }
        
        binding.falseRadioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val falseOption = question.options.find { it.text == "Неверно" }
                if (falseOption != null) {
                    viewModel.saveAnswer(listOf(falseOption.id))
                    updateFinishButtonState()
                }
            }
        }
    }
    
    /**
     * Сохранить текущий ответ пользователя
     */
    private fun saveCurrentAnswer() {
        val currentQuestion = viewModel.currentQuestion.value ?: return
        
        when (currentQuestion.type) {
            QuestionType.SINGLE_CHOICE -> {
                saveSingleChoiceAnswer(currentQuestion)
            }
            QuestionType.MULTIPLE_CHOICE -> {
                saveMultipleChoiceAnswer(currentQuestion)
            }
            QuestionType.TRUE_FALSE -> {
                saveTrueFalseAnswer(currentQuestion)
            }
        }
        
        // Обновляем состояние кнопки "Завершить" сразу после сохранения ответа
        updateFinishButtonState()
    }
    
    /**
     * Сохранить ответ на вопрос с одним вариантом
     */
    private fun saveSingleChoiceAnswer(question: QuizQuestion) {
        val checkedId = binding.singleChoiceGroup.checkedRadioButtonId
        
        if (checkedId != -1) {
            val selectedIndex = binding.singleChoiceGroup.indexOfChild(
                binding.singleChoiceGroup.findViewById(checkedId)
            )
            
            if (selectedIndex >= 0 && selectedIndex < question.options.size) {
                val selectedOption = question.options[selectedIndex]
                viewModel.saveAnswer(listOf(selectedOption.id))
            }
        }
    }
    
    /**
     * Сохранить ответ на вопрос с несколькими вариантами
     */
    private fun saveMultipleChoiceAnswer(question: QuizQuestion) {
        val selectedIds = mutableListOf<String>()
        
        for (i in 0 until binding.multipleChoiceGroup.childCount) {
            val checkbox = binding.multipleChoiceGroup.getChildAt(i) as? CheckBox
            
            if (checkbox != null && checkbox.isChecked && i < question.options.size) {
                selectedIds.add(question.options[i].id)
            }
        }
        
        if (selectedIds.isNotEmpty()) {
            viewModel.saveAnswer(selectedIds)
        }
    }
    
    /**
     * Сохранить ответ на вопрос "Правда/Ложь"
     */
    private fun saveTrueFalseAnswer(question: QuizQuestion) {
        if (binding.trueRadioButton.isChecked || binding.falseRadioButton.isChecked) {
            val isTrue = binding.trueRadioButton.isChecked
            
            // Находим соответствующий вариант
            val selectedOption = question.options.find { 
                (isTrue && it.text == "Верно") || (!isTrue && it.text == "Неверно")
            }
            
            if (selectedOption != null) {
                viewModel.saveAnswer(listOf(selectedOption.id))
            }
        }
    }
    
    /**
     * Обновить состояние кнопки "Завершить"
     */
    private fun updateFinishButtonState() {
        binding.finishButton.isEnabled = viewModel.areAllQuestionsAnswered()
    }
    
    /**
     * Показать диалог подтверждения выхода
     */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из теста")
            .setMessage("Вы уверены, что хотите выйти из теста? Весь прогресс будет потерян.")
            .setPositiveButton("Да") { _, _ ->
                findNavController().popBackStack()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
    
    /**
     * Показать диалог подтверждения завершения теста
     */
    private fun showFinishConfirmationDialog() {
        val remainingQuestionsCount = (viewModel.totalQuestions.value ?: 0) - viewModel.getAnsweredQuestionsCount()
        
        if (remainingQuestionsCount > 0) {
            AlertDialog.Builder(requireContext())
                .setTitle("Завершение теста")
                .setMessage("У вас остались неотвеченные вопросы ($remainingQuestionsCount). Вы уверены, что хотите завершить тест?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.finishTest()
                }
                .setNegativeButton("Нет", null)
                .show()
        } else {
            viewModel.finishTest()
        }
    }
    
    /**
     * Перейти к экрану с результатами
     */
    private fun navigateToResults() {
        val quiz = viewModel.quiz.value ?: return
        
        Toast.makeText(
            requireContext(),
            "Тест завершен! Переход к результатам...",
            Toast.LENGTH_SHORT
        ).show()
        
        // Создаем Bundle с аргументами
        val bundle = Bundle().apply {
            putString("quizId", quiz.id)
        }
        
        // Переходим к экрану результатов
        findNavController().navigate(
            R.id.quizResultsFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 