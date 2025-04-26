package uz.dckroff.pcap.features.glossary
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.DialogFragment
//import androidx.fragment.app.viewModels
//import dagger.hilt.android.AndroidEntryPoint
//import timber.log.Timber
//import uz.dckroff.pcap.R
//import uz.dckroff.pcap.databinding.FragmentGlossaryDetailBinding
//import uz.dckroff.pcap.data.model.GlossaryTerm
//
///**
// * Фрагмент для отображения деталей термина из глоссария
// */
//@AndroidEntryPoint
//class GlossaryDetailFragment2 : DialogFragment() {
//
//    private var _binding: FragmentGlossaryDetailBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel by viewModels<GlossaryDetailViewModel>()
//    private var termId: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
//        arguments?.let {
//            termId = it.getString(ARG_TERM_ID)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentGlossaryDetailBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupUI()
//        setupObservers()
//        loadTermDetails()
//    }
//
//    private fun setupUI() {
//        // Настройка кнопки назад
//        binding.toolbar.setNavigationOnClickListener {
//            dismiss()
//        }
//
//        // Настройка кнопки добавления в закладки
//        binding.fabBookmark.setOnClickListener {
//            viewModel.toggleBookmark()
//        }
//    }
//
//    private fun setupObservers() {
//        // Наблюдение за данными о термине
//        viewModel.term.observe(viewLifecycleOwner) { term ->
//            if (term != null) {
//                updateUI(term)
//            }
//        }
//
//        // Наблюдение за статусом закладки
//        viewModel.isBookmarked.observe(viewLifecycleOwner) { isBookmarked ->
//            updateBookmarkIcon(isBookmarked)
//        }
//
//        // Наблюдение за статусом загрузки
//        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//            binding.contentLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
//        }
//
//        // Наблюдение за сообщениями об ошибках
//        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
//            if (!errorMessage.isNullOrEmpty()) {
//                binding.errorMessage.text = errorMessage
//                binding.errorMessage.visibility = View.VISIBLE
//            } else {
//                binding.errorMessage.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun loadTermDetails() {
//        termId?.let {
//            viewModel.loadTermDetails(it)
//        } ?: run {
//            binding.errorMessage.text = getString(R.string.error_loading_terms)
//            binding.errorMessage.visibility = View.VISIBLE
//            Timber.e("Term ID is null")
//        }
//    }
//
//    private fun updateUI(term: GlossaryTerm) {
//        binding.apply {
//            toolbar.title = term.term
//            termTitle.text = term.term
//            termDefinition.text = term.definition
//            termCategory.text = term.category
//            // Другие поля UI в зависимости от структуры модели GlossaryTerm
//        }
//    }
//
//    private fun updateBookmarkIcon(isBookmarked: Boolean) {
//        binding.fabBookmark.setImageResource(
//            if (isBookmarked) R.drawable.ic_bookmark
//            else R.drawable.ic_bookmark
//        )
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    companion object {
//        private const val ARG_TERM_ID = "termId"
//
//        /**
//         * Создает новый экземпляр GlossaryDetailFragment
//         * @param termId идентификатор термина
//         * @return новый экземпляр GlossaryDetailFragment
//         */
//        fun newInstance(termId: String): GlossaryDetailFragment {
//            return GlossaryDetailFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_TERM_ID, termId)
//                }
//            }
//        }
//    }
//}