package uz.dckroff.pcap.features.glossary

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.GlossaryCategories
import uz.dckroff.pcap.databinding.FragmentGlossaryBinding

/**
 * Фрагмент для отображения списка терминов глоссария с возможностью поиска и фильтрации по категориям
 */
@AndroidEntryPoint
class GlossaryFragment : Fragment() {

    companion object {
        private const val TAG = "GlossaryFragment"
        
        // Для хранения статического экземпляра адаптера
        private var sharedAdapter: GlossaryAdapter? = null
    }

    private var _binding: FragmentGlossaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlossaryViewModel by viewModels()
    
    // Используем общий адаптер для всех экземпляров фрагмента
    private val adapter: GlossaryAdapter
        get() {
            if (sharedAdapter == null) {
                Log.d(TAG, "Создание нового глобального адаптера")
                sharedAdapter = createAdapter()
            }
            return sharedAdapter!!
        }
    
    // Флаг для отслеживания первой инициализации
    private var isFirstInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("$TAG: onCreate")
        
        // Установка флага для сохранения состояния фрагмента
        setRetainInstance(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("$TAG: onCreateView")
        _binding = FragmentGlossaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Timber.d("$TAG: onViewCreated, isFirstInit = $isFirstInit")
        
        // Отложенная инициализация тяжелых компонентов
        view.doOnPreDraw {
            setupRecyclerView()
            
            // Настраиваем компоненты только при первом создании
            if (isFirstInit) {
                setupSearchView()
                setupCategoriesChips()
                isFirstInit = false
            }
            
            // Наблюдаем за данными только если фрагмент активен
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                observeViewModel()
            }
        }
    }
    
    private fun createAdapter(): GlossaryAdapter {
        return GlossaryAdapter { term ->
            val bundle = Bundle().apply {
                putString("termId", term.id)
            }
            try {
                // Пробуем найти действие в main_nav_graph.xml
                findNavController().navigate(R.id.action_glossaryFragment_to_glossaryDetailFragment, bundle)
            } catch (e: Exception) {
                try {
                    // Если не получилось, пробуем действие из nav_graph.xml
                    findNavController().navigate(R.id.action_glossary_fragment_to_glossary_detail_fragment, bundle)
                } catch (e: Exception) {
                    // Если и это не работает, логируем ошибку
                    Timber.e(e, "Ошибка навигации: ${e.message}")
                    // Показываем сообщение пользователю
                    Toast.makeText(requireContext(), "Ошибка навигации", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // Оптимизированная настройка RecyclerView
        binding.recyclerView.apply {
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(requireContext())
            }
            
            if (adapter != this@GlossaryFragment.adapter) {
                adapter = this@GlossaryFragment.adapter
                
                // Оптимизируем работу RecyclerView
                setHasFixedSize(true)
                setItemViewCacheSize(20)
                recycledViewPool.setMaxRecycledViews(0, 20)
            }
            
            // Отключаем анимации для повышения производительности
            (itemAnimator as androidx.recyclerview.widget.SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchTerms(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchTerms(it) }
                return true
            }
        })
    }

    private fun setupCategoriesChips() {
        // Настройка чипов для категорий
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                val chip = group.findViewById<Chip>(chipId)
                val category = chip.text.toString()
                viewModel.setCategory(category)
            } else {
                viewModel.setCategory(GlossaryCategories.ALL)
            }
        }
    }

    private fun observeViewModel() {
        // Используем lifecycleScope для более эффективного управления подписками
        viewLifecycleOwner.lifecycleScope.launch {
            // Наблюдаем за категориями
            viewModel.categories.observe(viewLifecycleOwner) { categories ->
                Log.d(TAG, "Получено ${categories.size} категорий")
                if (binding.chipGroupCategories.childCount == 0) {
                    updateCategoriesChips(categories)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Наблюдаем за списком терминов
            viewModel.terms.observe(viewLifecycleOwner) { terms ->
                Log.d(TAG, "Получено ${terms.size} терминов")
                adapter.submitList(terms)
                updateVisibility(terms.isEmpty(), viewModel.loading.value ?: false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Наблюдаем за состоянием загрузки
            viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                updateVisibility(viewModel.terms.value?.isEmpty() ?: true, isLoading)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Наблюдаем за ошибками
            viewModel.error.observe(viewLifecycleOwner) { error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateVisibility(isEmpty: Boolean, isLoading: Boolean) {
        // Обновляем видимость компонентов в зависимости от состояния
        binding.recyclerView.visibility = if (!isEmpty && !isLoading) View.VISIBLE else View.GONE
//        binding.emptyView?.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
    }

    private fun updateCategoriesChips(categories: List<String>) {
        try {
            // Очищаем и обновляем чипы только если это необходимо
            binding.chipGroupCategories.removeAllViews()
            
            // Добавляем чипы для каждой категории
            categories.forEach { category ->
                val chip = layoutInflater.inflate(
                    R.layout.item_category_chip, 
                    binding.chipGroupCategories, 
                    false
                ) as Chip
                
                chip.text = category
                chip.id = View.generateViewId()
                
                // Выбираем чип, если он соответствует текущей выбранной категории
                viewModel.selectedCategory.value?.let {
                    if (it == category) {
                        chip.isChecked = true
                    }
                }
                
                binding.chipGroupCategories.addView(chip)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обновлении чипов категорий")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Timber.d("$TAG: onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Timber.d("$TAG: onPause")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("$TAG: onDestroyView")
        _binding = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("$TAG: onDestroy")
        
        // Очищаем адаптер только если фрагмент удаляется навсегда
        if (requireActivity().isFinishing) {
            sharedAdapter = null
        }
    }
} 