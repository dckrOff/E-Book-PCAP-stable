package uz.dckroff.pcap.features.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.MainActivity
import uz.dckroff.pcap.R
import uz.dckroff.pcap.databinding.FragmentDashboardBinding
import uz.dckroff.pcap.features.glossary.GlossaryDetailFragment
import uz.dckroff.pcap.ui.content.ContentListFragment
import uz.dckroff.pcap.ui.content.ContentListViewModel

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var recentAdapter: ChapterAdapter
    private lateinit var allChaptersAdapter: ChapterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupUI()
        observeViewModel()

        // Загрузка временных данных для демонстрации
        loadDummyData()
    }

    private fun setupAdapters() {
        // Инициализация адаптера для недавно просмотренных глав
        recentAdapter = ChapterAdapter(
            onChapterClicked = { chapter ->
                viewModel.onChapterClicked(chapter)
            },
            isWideLayout = false
        )

        // Инициализация адаптера для рекомендуемых глав
        allChaptersAdapter = ChapterAdapter(
            onChapterClicked = { chapter ->
                viewModel.onChapterClicked(chapter)
            },
            isWideLayout = true
        )
    }

    private fun setupUI() {
        // Настройка RecyclerView для недавно просмотренных материалов
        binding.rvRecentChapters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        // Настройка RecyclerView для рекомендованных материалов
        binding.rvAllChapters.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = allChaptersAdapter
        }

        // Обработка нажатия на кнопку "Продолжить обучение"
        binding.btnContinueLearning.setOnClickListener {
            viewModel.onContinueLearningClicked()
        }
    }

    // Функция для загрузки временных данных
    private fun loadDummyData() {
        // Установка общего прогресса
        binding.progressBar.progress = 45
        binding.tvProgressPercent.text = "45%"

        // Временные данные для недавно просмотренных глав
        val recentChapters = listOf(
            Chapter(
                id = "1",
                title = "Основы архитектуры параллельных вычислений",
                description = "Введение в параллельную архитектуру компьютеров",
                progress = 75,
                order = 1
            ),
            Chapter(
                id = "2",
                title = "Модели параллельного программирования",
                description = "Обзор основных моделей параллельного программирования",
                progress = 60,
                order = 2
            ),
            Chapter(
                id = "3",
                title = "Многоядерные процессоры",
                description = "Устройство и принципы работы многоядерных процессоров",
                progress = 30,
                order = 3
            ),
            Chapter(
                id = "1",
                title = "Основы архитектуры параллельных вычислений",
                description = "Введение в параллельную архитектуру компьютеров",
                progress = 75,
                order = 1
            ),
            Chapter(
                id = "2",
                title = "Модели параллельного программирования",
                description = "Обзор основных моделей параллельного программирования",
                progress = 60,
                order = 2
            ),
            Chapter(
                id = "3",
                title = "Многоядерные процессоры",
                description = "Устройство и принципы работы многоядерных процессоров",
                progress = 30,
                order = 3
            )
        )

        // Временные данные для рекомендуемых глав
        val recommendedChapters = listOf(
            Chapter(
                id = "4",
                title = "GPU программирование",
                description = "Основы программирования графических процессоров",
                progress = 10,
                order = 5
            ),
            Chapter(
                id = "5",
                title = "Векторные вычисления",
                description = "Принципы векторных вычислений и SIMD инструкции",
                progress = 5,
                order = 6
            ),
            Chapter(
                id = "6",
                title = "Оптимизация параллельных алгоритмов",
                description = "Методы оптимизации параллельных алгоритмов",
                progress = 0,
                order = 7
            ),
            Chapter(
                id = "4",
                title = "GPU программирование",
                description = "Основы программирования графических процессоров",
                progress = 10,
                order = 5
            ),
            Chapter(
                id = "5",
                title = "Векторные вычисления",
                description = "Принципы векторных вычислений и SIMD инструкции",
                progress = 5,
                order = 6
            ),
            Chapter(
                id = "6",
                title = "Оптимизация параллельных алгоритмов",
                description = "Методы оптимизации параллельных алгоритмов",
                progress = 0,
                order = 7
            )
        )

        // Отображение данных
        recentAdapter.submitList(recentChapters)
        allChaptersAdapter.submitList(recommendedChapters)
    }

    private fun observeViewModel() {
        // Наблюдение за общим прогрессом
        viewModel.overallProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }

        // Наблюдение за недавно просмотренными главами
        viewModel.recentChapters.observe(viewLifecycleOwner) { chapters ->
            recentAdapter.submitList(chapters)

            // Показываем или скрываем секцию в зависимости от наличия данных
            if (chapters.isNullOrEmpty()) {
                binding.tvRecentTitle.visibility = View.GONE
                binding.rvRecentChapters.visibility = View.GONE
            } else {
                binding.tvRecentTitle.visibility = View.VISIBLE
                binding.rvRecentChapters.visibility = View.VISIBLE
            }
        }

        // Наблюдение за рекомендуемыми главами
        viewModel.AllChapters.observe(viewLifecycleOwner) { chapters ->
            allChaptersAdapter.submitList(chapters)

            // Показываем или скрываем секцию в зависимости от наличия данных
            if (chapters.isNullOrEmpty()) {
                binding.tvAllTitle.visibility = View.GONE
                binding.rvAllChapters.visibility = View.GONE
            } else {
                binding.tvAllTitle.visibility = View.VISIBLE
                binding.rvAllChapters.visibility = View.VISIBLE
            }
        }

        // Наблюдение за событием навигации к чтению главы
        viewModel.navigateToReading.observe(viewLifecycleOwner) { chapter ->
            chapter?.let {
                // Используем MainActivity и ее NavController для навигации к списку контента главы
                (requireActivity() as? MainActivity)?.let { mainActivity ->
                    // Показываем детальный контент и навигируем к списку контента
                    Timber.d("Navigating to chapter content: ${chapter.id}")
                    
                    mainActivity.navController.navigate(
                        R.id.contentListFragment,
                        Bundle().apply {
                            putString("chapterId", chapter.id)
                            putString("chapterTitle", chapter.title)
                        }
                    )
                    
                    // Сбрасываем событие навигации
                    viewModel.onReadingNavigated()
                }
            }
        }

        // Наблюдение за статусом загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Можно показать индикатор загрузки
                Timber.d("Loading state: $isLoading")
            }
        }

        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Навигация к экрану содержания главы
     */
    private fun navigateToReading(chapter: Chapter) {
        try {
            // Создаем и показываем GlossaryDetailFragment как диалог
            val contentListFragment = ContentListFragment.newInstance()
            contentListFragment.show(parentFragmentManager, "content_list_fragment")

//            val action = DashboardFragmentDirections.actionDashboardFragmentToContentListFragment()
//            findNavController().navigate(action)

            // Передаем идентификатор главы в ViewModel для фильтрации содержимого
            val contentListViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())
                .get(ContentListViewModel::class.java)
            contentListViewModel.filterContentByChapter(chapter.id)

            Timber.d("Navigating to content list screen for chapter: ${chapter.title}")
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.navigation_error) + e.message,
                Toast.LENGTH_SHORT
            ).show()
            Timber.e(e, "Error navigating to content list fragment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 