package uz.dckroff.pcap.ui.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import uz.dckroff.pcap.MainActivity
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.ContentItem
import uz.dckroff.pcap.databinding.FragmentContentListBinding
import uz.dckroff.pcap.utils.UiState

/**
 * Фрагмент для отображения содержания учебника
 */
@AndroidEntryPoint
class ContentListFragment : Fragment() {

    private var _binding: FragmentContentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContentListViewModel by viewModels()
    private lateinit var sectionAdapter: SectionAdapter

    private val chapterId: String? by lazy {
        arguments?.getString("chapterId")
    }

    private val chapterTitle: String? by lazy {
        arguments?.getString("chapterTitle")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupBackStackButton()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        viewModel.loadContent(chapterId!!)
    }

    private fun setupToolbar() {
        binding.toolbar.title = chapterTitle
        binding.toolbar.setNavigationOnClickListener {
            // Используем NavController из MainActivity для навигации назад
            if (!findNavController().popBackStack()) {
                // Если нет предыдущего фрагмента в стеке, возвращаемся к основным разделам
                (requireActivity() as? MainActivity)?.showMainContent()
            }
        }
    }

    private fun setupBackStackButton(){
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!findNavController().popBackStack()) {
                        (requireActivity() as? MainActivity)?.showMainContent()
                    }
                }
            }
        )
    }

    private fun setupRecyclerView() {
        sectionAdapter = SectionAdapter { section ->
            Timber.d("Click on section: " + section.title)
            navigateToReadingForSection(section)
        }

        binding.rvContent.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContent(chapterId = chapterId!!, forceRefresh = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contentState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> showContent(state.data)
                    is UiState.Error -> showError(state.message)
                    is UiState.Empty -> showEmpty()
                }
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            swipeRefresh.isRefreshing = false
            rvContent.visibility = View.GONE
            errorView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }

    private fun showContent(sections: List<ContentItem.Section>) {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            rvContent.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }

        sectionAdapter.submitList(sections)
    }

    private fun showError(message: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            rvContent.visibility = View.GONE
            errorView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE

            tvErrorMessage.text = message
            btnRetry.setOnClickListener {
                viewModel.loadContent(chapterId = chapterId!!, forceRefresh = true)
            }
        }
    }

    private fun showEmpty() {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            rvContent.visibility = View.GONE
            errorView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        }
    }

    private fun navigateToReadingForSection(section: ContentItem.Section) {
        try {
            findNavController().navigate(
                R.id.readingFragment,
                Bundle().apply {
                    putString("sectionId", section.id)
                    putString("chapterId", section.chapterId)
                    putString("sectionTitle", section.title)
                }
            )
        } catch (e: Exception) {
            Timber.tag("TAG").e("ошибка навигации: " + e.message)
            Toast.makeText(
                requireContext(),
                getString(R.string.navigation_error) + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ContentListFragment {
            return ContentListFragment().apply {
                arguments = Bundle().apply {}
            }
        }
    }
} 