package uz.dckroff.pcap.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.Bookmark
import uz.dckroff.pcap.databinding.FragmentBookmarksBinding

@AndroidEntryPoint
class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var bookmarksAdapter: BookmarksAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("BookmarksFragment created")

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Инициализация адаптера
        bookmarksAdapter = BookmarksAdapter(
            onBookmarkClick = { bookmark ->
//                navigateToSection(bookmark)
            },
            onDeleteClick = { bookmark -> deleteBookmark(bookmark) }
        )

        // Устанавливаем адаптер и LayoutManager
        binding.recyclerView.apply {
            adapter = bookmarksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }

        // Настройка обработчиков ошибок
        binding.errorView.buttonRetry.setOnClickListener {
            viewModel.loadBookmarks()
        }

        // Настройка пустого сообщения
        binding.emptyView.messageTextView.text = getString(R.string.no_bookmarks)
    }

    private fun setupObservers() {
        // Наблюдение за списком закладок
        viewModel.bookmarks.observe(viewLifecycleOwner) { bookmarks ->
            bookmarksAdapter.submitList(bookmarks)
        }

        // Наблюдение за состоянием загрузки
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.isVisible = isLoading
        }

        // Наблюдение за состоянием ошибки
        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.errorView.root.isVisible = error != null
            error?.let {
                binding.errorView.textErrorMessage.text = it
            }
        }

        // Наблюдение за пустым состоянием
        viewModel.empty.observe(viewLifecycleOwner) { isEmpty ->
            binding.emptyView.root.isVisible =
                isEmpty && !binding.progressBar.isVisible && !binding.errorView.root.isVisible
        }
    }

    /**
     * Переход к разделу по закладке
     */
    private fun navigateToSection(bookmark: Bookmark) {
        Timber.d("Переход к разделу: ${bookmark.sectionTitle}")
        try {
            // Переход к фрагменту чтения с ID раздела
            findNavController().navigate(
                R.id.readingFragment,
                Bundle().apply {
                    putString("subchapterId", bookmark.sectionId)
                    putString("subchapterTitle", bookmark.sectionTitle)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при переходе к разделу: ${e.message}")
            Snackbar.make(
                binding.root,
                "Не удалось открыть раздел",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Удаление закладки
     */
    private fun deleteBookmark(bookmark: Bookmark) {
        Timber.d("Удаление закладки: ${bookmark.sectionTitle}")
        viewModel.deleteBookmark(bookmark.id)

        Snackbar.make(
            binding.root,
            "Закладка удалена",
            Snackbar.LENGTH_LONG
        ).setAction("Отмена") {
            Timber.d("Отмена удаления закладки не реализована")
            // В полной реализации здесь был бы код восстановления закладки
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 