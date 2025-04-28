package uz.dckroff.pcap.features.reading

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.SectionContent
import uz.dckroff.pcap.databinding.FragmentReadingBinding

/**
 * Фрагмент для отображения учебного материала
 */
@AndroidEntryPoint
class ReadingFragment : Fragment() {

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReadingViewModel by viewModels()

    // Текущий ID раздела
    private val sectionId: String? by lazy {
        arguments?.getString("sectionId")
    }

    // Текущий ID раздела
    private val chapterId: String? by lazy {
        arguments?.getString("chapterId")
    }

    // Текущий ID раздела
    private val sectionTitle: String? by lazy {
        arguments?.getString("sectionTitle")
    }

    private var currentSectionId: String = ""

    // В onCreateView или onViewCreated
    private lateinit var contentRenderer: ContentRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Сохраняем ID текущего раздела
        currentSectionId = sectionId.toString()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupObservers()
        setupButtons()

        // Показываем прогресс перед загрузкой
        binding.progressBar.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE

        contentRenderer = ContentRenderer(requireContext(), binding.contentContainer)
        // Загружаем раздел
        Timber.tag("PCAP_READING").e("Загрузка раздела в фрагменте: $sectionTitle")
        viewModel.loadSection(chapterId = chapterId.toString(), sectionId = currentSectionId)

    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.title = sectionTitle
    }

    private fun setupObservers() {
        viewModel.currentSection.observe(viewLifecycleOwner) { section ->
            Timber.tag("PCAP_READING").e("Получен раздел в фрагменте: ${section.title}")
            binding.tvTitle.text = section.title
            binding.toolbar.title = section.title

            // Очищаем и добавляем новый контент
            binding.contentContainer.removeAllViews()
//            addDemoContent(section.id)

            // Показываем контент и скрываем прогресс
            binding.progressBar.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE

            // Сохраняем ID текущего раздела
            currentSectionId = section.id

            // Обновляем видимость кнопки перехода к следующему разделу
            updateNextButtonVisibility()
        }
        viewModel.sectionContent.observe(viewLifecycleOwner) { sectionContent ->
            Timber.d("Контент раздела: ${sectionContent.size}")
            addContentToScreen(sectionContent)
        }
    }

    private fun setupButtons() {
        // Кнопка закладки
        binding.fabBookmark.setOnClickListener {
            Toast.makeText(requireContext(), "Закладка добавлена", Toast.LENGTH_SHORT).show()
            Timber.d("Bookmark added for: ${sectionId}")
        }

        // Кнопка перехода к следующему разделу
        binding.fabNextSection.setOnClickListener {
            navigateToNextSection()
        }
    }

    /**
     * Обновляет видимость кнопки перехода к следующему разделу
     */
    private fun updateNextButtonVisibility() {
        val nextSection = viewModel.getNextSection(currentSectionId)
        binding.fabNextSection.visibility = if (nextSection != null) View.VISIBLE else View.GONE
        Timber.tag("PCAP_READING")
            .e("Обновлена видимость кнопки 'Далее'. Следующий раздел: ${nextSection?.title ?: "нет"}")
    }

    /**
     * Переход к следующему разделу
     */
    private fun navigateToNextSection() {
        val nextSection = viewModel.getNextSection(currentSectionId)
        if (nextSection != null) {
            // Отмечаем текущий раздел как прочитанный
            viewModel.markSectionAsRead(currentSectionId)

            // Переходим к следующему разделу
            try {
                Timber.tag("PCAP_READING")
                    .e("Переход к следующему разделу в фрагменте: ${nextSection.id}")

                // Сначала показываем прогресс
                binding.progressBar.visibility = View.VISIBLE
                binding.contentContainer.visibility = View.GONE

                // Используем прямой переход по ID вместо действия
                findNavController().navigate(
                    R.id.readingFragment,
                    Bundle().apply {
                        putString("subchapterId", nextSection.id)
                        putString("subchapterTitle", nextSection.title)
                    }
                )
            } catch (e: Exception) {
                Timber.tag("PCAP_READING").e(e, "Ошибка при навигации к следующему разделу")
                Toast.makeText(
                    requireContext(),
                    "Ошибка навигации: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Если это последний раздел, просто отмечаем его как прочитанный
            viewModel.markSectionAsRead(currentSectionId)
            Timber.tag("PCAP_READING").e("Достигнут последний раздел")
            Toast.makeText(requireContext(), "Вы достигли конца главы", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addContentToScreen(sectionContent: List<SectionContent>) {
        try {
            Timber.tag("PCAP_READING").e("Добавление контента для раздела: $sectionId")
            binding.contentContainer.removeAllViews()

            // Если фрагмент не привязан к активности, выходим
            if (!isAdded) {
                Timber.w("Фрагмент не привязан к активности, пропускаем добавление контента")
                return
            }

            // Добавляем разные типы контента на экран
            sectionContent.forEach { content ->
                when (content) {
                    is SectionContent.Text -> addTextContent(content)
                    is SectionContent.Image -> addImageContent(content)
                    is SectionContent.Formula -> addFormulaContent(content)
                    is SectionContent.Code -> addCodeContent(content)
                    is SectionContent.Video -> addVideoContent(content)
                    is SectionContent.Table -> addTableContent(content)
                    is SectionContent.Diagram -> contentRenderer.addDiagram(content)
                    else -> {
                        Timber.w("Неподдерживаемый тип контента: ${content::class.java.simpleName}")
                        // Можно добавить заглушку для неподдерживаемых типов
                    }
                }
            }

            Timber.tag("PCAP_READING").e("Контент успешно добавлен для раздела: $sectionId")
        } catch (e: Exception) {
            Timber.tag("PCAP_READING")
                .e(e, "Ошибка при добавлении контента для раздела: $sectionId")

            // Показываем сообщение об ошибке
            if (isAdded) {
                val errorTextView = TextView(requireContext()).apply {
                    text = "Произошла ошибка при загрузке контента.\nПожалуйста, попробуйте позже."
                    textSize = 16f
                    setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                binding.contentContainer.addView(errorTextView)
            }
        }
    }


    /**
     * Добавляет текстовый контент на экран
     */
    private fun addTextContent(content: SectionContent.Text) {
        val textView = TextView(requireContext()).apply {
            text = HtmlCompat.fromHtml(content.content, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textSize = 16f
            setPadding(0, 8, 0, 8)

            // Выделение текста, если нужно
            if (content.isHighlighted) {
                setBackgroundColor(resources.getColor(R.color.text_color_easy, null))
                setPadding(16, 16, 16, 16)
            }
        }
        binding.contentContainer.addView(textView)
    }

    /**
     * Добавляет изображение на экран
     */
    private fun addImageContent(content: SectionContent.Image) {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_content_image, binding.contentContainer, false)

        val imageView = containerView.findViewById<ImageView>(R.id.ivContentImage)
        val captionView = containerView.findViewById<TextView>(R.id.tvImageCaption)

        // Загрузка изображения (использовать Glide или другую библиотеку)
        // В демо-версии можно использовать плейсхолдер
        imageView.setImageResource(R.drawable.img_placeholder)

        // Установка подписи, если она есть
        if (content.caption.isNotEmpty()) {
            captionView.text = content.caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }

        binding.contentContainer.addView(containerView)
    }

    /**
     * Добавляет формулу на экран
     */
    private fun addFormulaContent(content: SectionContent.Formula) {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_content_formula, binding.contentContainer, false)

        val formulaView = containerView.findViewById<TextView>(R.id.tvFormula)
        val captionView = containerView.findViewById<TextView>(R.id.tvFormulaCaption)

        // В реальном приложении здесь бы использовался MathJax или KaTeX
        // Для демонстрации просто отображаем текст формулы
        formulaView.text = content.content

        // Установка стиля в зависимости от типа формулы
        if (content.isInline) {
            formulaView.textSize = 16f
            formulaView.gravity = android.view.Gravity.START
        } else {
            formulaView.textSize = 20f
            formulaView.gravity = android.view.Gravity.CENTER
        }

        // Установка подписи, если она есть
        if (content.caption.isNotEmpty()) {
            captionView.text = content.caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }

        binding.contentContainer.addView(containerView)
    }

    /**
     * Добавляет блок кода на экран
     */
    private fun addCodeContent(content: SectionContent.Code) {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_content_code, binding.contentContainer, false)

        val codeView = containerView.findViewById<TextView>(R.id.tvCodeContent)
        val captionView = containerView.findViewById<TextView>(R.id.tvCodeTitle)

        // Отображение кода
        codeView.text = content.content
        codeView.typeface = Typeface.MONOSPACE

        // В реальном приложении можно использовать библиотеку подсветки синтаксиса

        // Установка подписи, если она есть
        if (content.caption.isNotEmpty()) {
            captionView.text = content.caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }

        binding.contentContainer.addView(containerView)
    }

    /**
     * Добавляет видео на экран
     */
    private fun addVideoContent(content: SectionContent.Video) {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_content_video, binding.contentContainer, false)

        val thumbnailView = containerView.findViewById<ImageView>(R.id.videoThumbnail)
        val playButton = containerView.findViewById<ImageView>(R.id.playButton)
        val captionView = containerView.findViewById<TextView>(R.id.videoTitle)
        val durationView = containerView.findViewById<TextView>(R.id.tv_duration)

        // Загрузка превью (используйте Glide или другую библиотеку)
        thumbnailView.setImageResource(R.drawable.img_placeholder)

        // Отображение длительности
        if (content.durationSeconds > 0) {
            val minutes = content.durationSeconds / 60
            val seconds = content.durationSeconds % 60
            durationView.text = String.format("%d:%02d", minutes, seconds)
            durationView.visibility = View.VISIBLE
        } else {
            durationView.visibility = View.GONE
        }

        // Обработка нажатия на кнопку воспроизведения
        playButton.setOnClickListener {
            // Здесь был бы код для воспроизведения видео
            Toast.makeText(
                requireContext(),
                "Воспроизведение видео: ${content.url}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Установка подписи, если она есть
        if (content.caption.isNotEmpty()) {
            captionView.text = content.caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }

        binding.contentContainer.addView(containerView)
    }

    /**
     * Добавляет таблицу на экран
     */
    private fun addTableContent(content: SectionContent.Table) {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_content_table, binding.contentContainer, false)

        val tableLayout = containerView.findViewById<TableLayout>(R.id.tableLayout)
        val captionView = containerView.findViewById<TextView>(R.id.tvCaption)

        // Создание заголовка таблицы
        val headerRow = TableRow(requireContext())
        content.headers.forEach { headerText ->
            val headerView = TextView(requireContext()).apply {
                text = headerText
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 8, 16, 8)
                background =
                    ResourcesCompat.getDrawable(resources, R.drawable.table_header_bg, null)
            }
            headerRow.addView(headerView)
        }
        tableLayout.addView(headerRow)

        // Создание строк с данными
        content.rows.forEach { rowData ->
            val dataRow = TableRow(requireContext())
            rowData.forEach { cellText ->
                val cellView = TextView(requireContext()).apply {
                    text = cellText
                    setPadding(16, 8, 16, 8)
                    background =
                        ResourcesCompat.getDrawable(resources, R.drawable.table_cell_bg, null)
                }
                dataRow.addView(cellView)
            }
            tableLayout.addView(dataRow)
        }

        // Установка подписи, если она есть
        if (content.caption.isNotEmpty()) {
            captionView.text = content.caption
            captionView.visibility = View.VISIBLE
        } else {
            captionView.visibility = View.GONE
        }

        binding.contentContainer.addView(containerView)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reading, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_note -> {
                // В дальнейшем здесь будет код для добавления заметки
                Toast.makeText(requireContext(), "Добавление заметки", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_share -> {
                // В дальнейшем здесь будет код для шеринга
                Toast.makeText(requireContext(), "Поделиться", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_font_settings -> {
                // В дальнейшем здесь будет код для настройки шрифта
                Toast.makeText(requireContext(), "Настройки шрифта", Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 