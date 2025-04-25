package uz.dckroff.pcap.ui.content

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private lateinit var contentAdapter: ContentAdapter
    
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
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        
        viewModel.loadContent()
    }
    
    private fun setupRecyclerView() {
        contentAdapter = ContentAdapter(
            onChapterClick = { chapter ->
                navigateToReadingForChapter(chapter)
            },
            onSubchapterClick = { subchapter ->
                navigateToReading(subchapter)
            }
        )
        
        binding.rvContent.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContent(forceRefresh = true)
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
    
    private fun showContent(chapters: List<ContentItem.Chapter>) {
        binding.apply {
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            rvContent.visibility = View.VISIBLE
            errorView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
        
        contentAdapter.submitChaptersWithSubchapters(chapters)
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
                viewModel.loadContent(true)
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
    
    private fun navigateToReading(subchapter: ContentItem.Subchapter) {
        try {
            val action = ContentListFragmentDirections.actionContentListFragmentToReadingFragment(
                subchapterId = subchapter.id,
                subchapterTitle = subchapter.title
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("TAG","Ошибка навигации: " + e.message)
            Toast.makeText(
                requireContext(),
                getString(R.string.navigation_error) + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun navigateToReadingForChapter(chapter: ContentItem.Chapter) {
        try {
            val action = ContentListFragmentDirections.actionContentListFragmentToReadingFragment(
                subchapterId = chapter.id,
                subchapterTitle = chapter.title
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("TAG","Ошибка навигации: " + e.message)
            Toast.makeText(
                requireContext(),
                getString(R.string.navigation_error) + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun navigateToReadingForSection(section: ContentItem.Section) {
        try {
            val action = ContentListFragmentDirections.actionContentListFragmentToReadingFragment(
                subchapterId = section.id,
                subchapterTitle = section.title
            )
            findNavController().navigate(action)
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
} 