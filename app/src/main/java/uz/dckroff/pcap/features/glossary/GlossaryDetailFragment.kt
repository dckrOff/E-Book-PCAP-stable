package uz.dckroff.pcap.features.glossary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.databinding.FragmentGlossaryDetailBinding
import uz.dckroff.pcap.utils.Resource
import uz.dckroff.pcap.utils.showErrorSnackbar

/**
 * Фрагмент для отображения детальной информации о термине из глоссария
 */
@AndroidEntryPoint
class GlossaryDetailFragment : Fragment() {

    private var _binding: FragmentGlossaryDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlossaryViewModel by viewModels()
    private val args: GlossaryDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlossaryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadTermDetails()
        observeTermDetails()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.glossary_term_details)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadTermDetails() {
        viewModel.getTermById(args.termId)
    }

    private fun observeTermDetails() {
        viewModel.currentTerm.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.errorLayout.root.isVisible = false
                    binding.contentLayout.isVisible = false
                }

                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.errorLayout.root.isVisible = false
                    binding.contentLayout.isVisible = true

                    resource.data?.let { term ->
                        binding.textTitle.text = term.term
                        binding.chipCategory.text = term.category
                        binding.textDefinition.text = term.definition

                        // Загрузка связанных терминов
                        binding.relatedTermsLayout.isVisible = term.relatedTerms.isNotEmpty()
                        binding.chipGroupRelatedTerms.removeAllViews()

                        term.relatedTerms.forEach { relatedTermId ->
                            // Получаем термин из кэша, если возможно
                            val relatedTermText =
                                viewModel.getTermByIdSync(relatedTermId)?.term ?: relatedTermId

                            // Создаем чип для каждого связанного термина
                            val chip = Chip(requireContext()).apply {
                                text = relatedTermText
                                isClickable = true
                                setOnClickListener {
                                    // Используем прямую навигацию с Bundle вместо Directions
                                    val bundle = Bundle().apply {
                                        putString("termId", relatedTermId)
                                    }
                                    try {
                                        // Пробуем найти действие в main_nav_graph.xml
                                        findNavController().navigate(
                                            R.id.action_glossaryDetail_self,
                                            bundle
                                        )
                                    } catch (e: Exception) {
                                        try {
                                            // Если не получилось, пробуем действие из nav_graph.xml
                                            findNavController().navigate(
                                                R.id.action_glossary_detail_self,
                                                bundle
                                            )
                                        } catch (e2: Exception) {
                                            try {
                                                // Последняя попытка - реиспользовать то же самое назначение
                                                findNavController().navigate(
                                                    R.id.glossaryDetailFragment,
                                                    bundle
                                                )
                                            } catch (e3: Exception) {
                                                // Если и это не работает, логируем ошибку
                                                Timber.e(e3, "Ошибка навигации: ${e3.message}")
                                                // Показываем сообщение пользователю
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Ошибка навигации",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            }
                            binding.chipGroupRelatedTerms.addView(chip)
                        }

                        // Загрузка связанных разделов
                        binding.relatedSectionsLayout.isVisible =
                            term.relatedSectionIds.isNotEmpty()
                        binding.chipGroupRelatedSections.removeAllViews()

                        term.relatedSectionIds.forEach { sectionId ->
                            val sectionTitle =
                                "Раздел $sectionId" // Здесь позже можно добавить получение реального заголовка раздела
                            val chip = Chip(requireContext()).apply {
                                text = sectionTitle
                                isClickable = true
                                setOnClickListener {
                                    // Навигация к разделу (будет добавлена позже)
                                }
                            }
                            binding.chipGroupRelatedSections.addView(chip)
                        }
                    }
                }

                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.contentLayout.isVisible = false
                    binding.errorLayout.root.isVisible = true
                    binding.errorLayout.textErrorMessage.text =
                        resource.message ?: "Ошибка загрузки данных"
                    binding.errorLayout.buttonRetry.setOnClickListener {
                        loadTermDetails()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 