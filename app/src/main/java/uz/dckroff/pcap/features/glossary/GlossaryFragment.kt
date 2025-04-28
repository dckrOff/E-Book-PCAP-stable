package uz.dckroff.pcap.features.glossary

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.MainActivity
import uz.dckroff.pcap.R
import uz.dckroff.pcap.databinding.FragmentGlossaryBinding
import uz.dckroff.pcap.utils.showErrorSnackbar

/**
 * Фрагмент для отображения списка терминов глоссария с возможностью поиска и фильтрации по категориям
 */
@AndroidEntryPoint
class GlossaryFragment : Fragment() {

    private var _binding: FragmentGlossaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlossaryViewModel by viewModels()
    private lateinit var adapter: GlossaryAdapter
    private var isRecyclerViewInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false) // Отключаем меню опций для ViewPager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlossaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isRecyclerViewInitialized) {
            setupRecyclerView()
            isRecyclerViewInitialized = true
        }

        setupSearchView()
        setupCategoriesChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // Создаем адаптер с пустой функцией обработки нажатий
        adapter = GlossaryAdapter { term ->
            viewModel.loadTerm(term.id)
            // Переход к экрану деталей термина
            findNavController().navigate(R.id.action_glossaryFragment_to_glossaryDetailFragment)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GlossaryFragment.adapter
        }

        // Настройка SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTerms()
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        binding.searchView.setOnCloseListener {
            viewModel.clearSearchQuery()
            false
        }
    }

    private fun setupCategoriesChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId != null) {
                val chip = group.findViewById<Chip>(checkedId)
                val category = chip.text.toString()
                viewModel.setCategory(category)
            } else {
                // Если ни один чип не выбран, показываем все категории
                viewModel.setCategory(GlossaryViewModel.ALL_CATEGORIES)
            }
        }
    }

    private fun observeViewModel() {
        // Наблюдаем за списком категорий
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            Log.e("TAG", "observeViewModel: ")
            binding.chipGroupCategories.removeAllViews()

            // Добавляем чип "Все" в начало
            val allChip = Chip(requireContext()).apply {
                text = GlossaryViewModel.ALL_CATEGORIES
                isCheckable = true
                isChecked = viewModel.selectedCategory.value == GlossaryViewModel.ALL_CATEGORIES
            }
            binding.chipGroupCategories.addView(allChip)

            // Добавляем остальные категории
            categories.forEach { category ->
                if (category != GlossaryViewModel.ALL_CATEGORIES) {
                    val chip = Chip(requireContext()).apply {
                        text = category
                        isCheckable = true
                        isChecked = viewModel.selectedCategory.value == category
                    }
                    binding.chipGroupCategories.addView(chip)
                }
            }
        }

        // Наблюдаем за списком терминов
        viewModel.terms.observe(viewLifecycleOwner) { terms ->
            adapter.submitList(terms)
            binding.emptyView.isVisible = terms.isEmpty()
        }

        // Наблюдаем за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.isVisible = isLoading && adapter.itemCount == 0
        }

        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                binding.root.showErrorSnackbar(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 