package uz.dckroff.pcap.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.QuizDifficulty
import uz.dckroff.pcap.databinding.FragmentQuizListBinding

@AndroidEntryPoint
class QuizListFragment : Fragment() {

    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizListViewModel by viewModels()
    private lateinit var quizAdapter: QuizAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Timber.d("QuizListFragment created")
        
        setupRecyclerView()
        setupFilters()
        setupObservers()
    }
    
    private fun setupRecyclerView() {
        quizAdapter = QuizAdapter(
            onQuizClicked = { quiz ->
                Timber.d("Тест выбран: ${quiz.title}")
                // Переход к экрану сессии теста
                val bundle = Bundle().apply {
                    putString("quizId", quiz.id)
                }
                findNavController().navigate(
                    R.id.quizSessionFragment,
                    bundle
                )
            },
            onResultsClicked = { quiz ->
                Timber.d("Просмотр результатов теста: ${quiz.title}")
                // Переход к экрану результатов
                val bundle = Bundle().apply {
                    putString("quizId", quiz.id)
                }
                findNavController().navigate(
                    R.id.quizResultsFragment,
                    bundle
                )
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quizAdapter
        }
    }
    
    private fun setupFilters() {
        // Фильтр по статусу
        binding.allQuizzesChip.setOnClickListener {
            viewModel.setStatusFilter(QuizListViewModel.StatusFilter.ALL)
        }
        
        binding.completedQuizzesChip.setOnClickListener {
            viewModel.setStatusFilter(QuizListViewModel.StatusFilter.COMPLETED)
        }
        
        binding.pendingQuizzesChip.setOnClickListener {
            viewModel.setStatusFilter(QuizListViewModel.StatusFilter.PENDING)
        }
        
        // Фильтр по сложности
        binding.allDifficultyChip.setOnClickListener {
            viewModel.setDifficultyFilter(null)
        }
        
        binding.easyDifficultyChip.setOnClickListener {
            viewModel.setDifficultyFilter(QuizDifficulty.EASY)
        }
        
        binding.mediumDifficultyChip.setOnClickListener {
            viewModel.setDifficultyFilter(QuizDifficulty.MEDIUM)
        }
        
        binding.hardDifficultyChip.setOnClickListener {
            viewModel.setDifficultyFilter(QuizDifficulty.HARD)
        }
    }
    
    private fun setupObservers() {
        // Наблюдаем за отфильтрованным списком тестов
        viewModel.filteredQuizzes.observe(viewLifecycleOwner) { quizzes ->
            quizAdapter.submitList(quizzes)
            
            // Показываем пустой экран, если список пуст
            if (quizzes.isEmpty()) {
                binding.emptyView.root.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.root.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
            
            Timber.d("Отображено ${quizzes.size} тестов")
        }
        
        // Наблюдаем за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            // Скрываем RecyclerView во время загрузки
            if (isLoading) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.root.visibility = View.GONE
                binding.errorView.root.visibility = View.GONE
            }
        }
        
        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.errorView.root.visibility = View.VISIBLE
                binding.errorView.textErrorMessage.text = errorMessage
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.root.visibility = View.GONE
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