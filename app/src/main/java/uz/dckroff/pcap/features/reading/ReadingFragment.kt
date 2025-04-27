package uz.dckroff.pcap.features.reading

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.DiagramElement
import uz.dckroff.pcap.data.model.SectionContent
import uz.dckroff.pcap.databinding.FragmentReadingBinding

/**
 * Фрагмент для отображения учебного материала
 */
@AndroidEntryPoint
class ReadingFragment : Fragment() {

    private var _binding: FragmentReadingBinding? = null
    private val binding get() = _binding!!

    private val args: ReadingFragmentArgs by navArgs()
    private val viewModel: ReadingViewModel by viewModels()

    // Текущий ID раздела
    private var currentSectionId: String = ""

    // В onCreateView или onViewCreated
    private lateinit var contentRenderer: ContentRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Сохраняем ID текущего раздела
        currentSectionId = args.subchapterId

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
        Timber.tag("PCAP_READING").e("Загрузка раздела в фрагменте: $currentSectionId")
        viewModel.loadSection(currentSectionId)

    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.title = args.subchapterTitle
    }

    private fun setupObservers() {
        viewModel.currentSection.observe(viewLifecycleOwner) { section ->
            Timber.tag("PCAP_READING").e("Получен раздел в фрагменте: ${section.title}")
            binding.tvTitle.text = section.title
            binding.toolbar.title = section.title

            // Очищаем и добавляем новый контент
            binding.contentContainer.removeAllViews()
            addDemoContent(section.id)

            // Показываем контент и скрываем прогресс
            binding.progressBar.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE

            // Сохраняем ID текущего раздела
            currentSectionId = section.id

            // Обновляем видимость кнопки перехода к следующему разделу
            updateNextButtonVisibility()
        }
    }

    private fun setupButtons() {
        // Кнопка закладки
        binding.fabBookmark.setOnClickListener {
            Toast.makeText(requireContext(), "Закладка добавлена", Toast.LENGTH_SHORT).show()
            Timber.d("Bookmark added for: ${args.subchapterId}")
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

    private fun addDemoContent(sectionId: String) {
        try {
            Timber.tag("PCAP_READING").e("Добавление контента для раздела: $sectionId")
            binding.contentContainer.removeAllViews()

            // Получаем список элементов контента для этого раздела
            val contentItems = getDemoContentForSection(sectionId)

            // Если фрагмент не привязан к активности, выходим
            if (!isAdded) {
                Timber.w("Фрагмент не привязан к активности, пропускаем добавление контента")
                return
            }

            // Добавляем разные типы контента на экран
            contentItems.forEach { content ->
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
     * Возвращает демонстрационный контент для раздела по его ID
     */
    private fun getDemoContentForSection(sectionId: String): List<SectionContent> {
        Log.e("TAG", "sectionId" + sectionId)
        return when (sectionId) {
            "1.1" -> listOf(
                SectionContent.Text(
                    id = "1.1.1",
                    content = "<p><strong>История развития параллельных вычислений</strong></p>" +
                            "<p>Параллельные вычисления имеют долгую историю, начиная с первых экспериментов " +
                            "в 1960-х годах и до современных суперкомпьютеров и многоядерных процессоров.</p>"
                ),
                SectionContent.Image(
                    id = "1.1.2",
                    url = "https://example.com/parallel_computing_history.jpg",
                    caption = "Эволюция параллельных вычислений",
                    description = "График развития параллельных вычислительных систем с 1960 по 2023 год"
                ),
                SectionContent.Text(
                    id = "1.1.3",
                    content = "<p>Основные этапы развития параллельных вычислений включают:</p>" +
                            "<ul><li>1960-е: Первые экспериментальные параллельные системы</li>" +
                            "<li>1970-е: Появление векторных процессоров</li>" +
                            "<li>1980-е: Развитие массивно-параллельных систем</li>" +
                            "<li>1990-е: Кластерные вычисления и распределенные системы</li>" +
                            "<li>2000-е: Многоядерные процессоры и GPGPU</li>" +
                            "<li>2010-е: Гетерогенные вычислительные системы</li>" +
                            "<li>2020-е: Квантовые вычисления и специализированные ускорители</li></ul>"
                ),
                SectionContent.Code(
                    id = "1.1.4",
                    content = """
                        // Пример раннего параллельного кода (1980-е)
                        #pragma parallel
                        for (int i = 0; i < N; i++) {
                            result[i] = compute(data[i]);
                        }
                    """.trimIndent(),
                    language = "c",
                    caption = "Пример директивы параллелизма для компилятора (1980-е годы)"
                )
            )

            "1.2" -> listOf(
                SectionContent.Text(
                    id = "1.2.1",
                    content = "<p><strong>Основные понятия параллельных вычислений</strong></p>" +
                            "<p>Параллельные вычисления основаны на ряде ключевых концепций, " +
                            "которые необходимо понимать для эффективного использования " +
                            "параллельных систем.</p>"
                ),
                SectionContent.Diagram(
                    id = "1.2.2",
                    elements = listOf(
                        DiagramElement(
                            id = "node1",
                            type = "block",
                            text = "Задача",
                            x = 50f,
                            y = 50f,
                            width = 100f,
                            height = 50f,
                            connections = listOf("node2")
                        ),
                        DiagramElement(
                            id = "node2",
                            type = "block",
                            text = "Декомпозиция",
                            x = 50f,
                            y = 150f,
                            width = 100f,
                            height = 50f,
                            connections = listOf("node3", "node4", "node5")
                        ),
                        DiagramElement(
                            id = "node3",
                            type = "block",
                            text = "Подзадача 1",
                            x = 0f,
                            y = 250f,
                            width = 80f,
                            height = 50f,
                            connections = listOf("node6")
                        ),
                        DiagramElement(
                            id = "node4",
                            type = "block",
                            text = "Подзадача 2",
                            x = 100f,
                            y = 250f,
                            width = 80f,
                            height = 50f,
                            connections = listOf("node6")
                        ),
                        DiagramElement(
                            id = "node5",
                            type = "block",
                            text = "Подзадача 3",
                            x = 200f,
                            y = 250f,
                            width = 80f,
                            height = 50f,
                            connections = listOf("node6")
                        ),
                        DiagramElement(
                            id = "node6",
                            type = "block",
                            text = "Агрегация результатов",
                            x = 50f,
                            y = 350f,
                            width = 150f,
                            height = 50f
                        )
                    ),
                    caption = "Схема параллельной обработки задачи"
                ),
                SectionContent.Text(
                    id = "1.2.3",
                    content = "<p><strong>Ключевые термины:</strong></p>" +
                            "<ul><li><strong>Параллелизм</strong> - одновременное выполнение вычислений</li>" +
                            "<li><strong>Масштабируемость</strong> - способность системы увеличивать производительность при добавлении ресурсов</li>" +
                            "<li><strong>Гранулярность</strong> - размер задач, выполняемых параллельно</li>" +
                            "<li><strong>Синхронизация</strong> - координация параллельных задач</li>" +
                            "<li><strong>Коммуникация</strong> - обмен данными между параллельными процессами</li></ul>"
                ),
                SectionContent.Formula(
                    id = "1.2.4",
                    content = "S_p = \\frac{T_1}{T_p}",
                    caption = "Ускорение параллельного алгоритма",
                    isInline = false
                )
            )

            "1.3" -> listOf(
                SectionContent.Text(
                    id = "1.3.1",
                    content = "<p><strong>Законы Амдала и Густафсона</strong></p>" +
                            "<p>Законы Амдала и Густафсона являются фундаментальными принципами, " +
                            "определяющими теоретические пределы ускорения, которое может быть " +
                            "достигнуто при использовании параллельных вычислений.</p>"
                ),
                SectionContent.Formula(
                    id = "1.3.2",
                    content = "S(n) = \\frac{1}{(1-p) + \\frac{p}{n}}",
                    caption = "Закон Амдала",
                    isInline = false
                ),
                SectionContent.Text(
                    id = "1.3.3",
                    content = "<p>где:<br>" +
                            "S(n) - ускорение при использовании n процессоров<br>" +
                            "p - доля программы, которая может быть распараллелена<br>" +
                            "(1-p) - доля последовательных вычислений</p>"
                ),
                SectionContent.Image(
                    id = "1.3.4",
                    url = "https://example.com/amdahl_law_graph.jpg",
                    caption = "График зависимости ускорения от числа процессоров согласно закону Амдала",
                    description = "Графики для различных значений p (доли параллельного кода)"
                ),
                SectionContent.Formula(
                    id = "1.3.5",
                    content = "S(n) = (1-p) + n \\cdot p",
                    caption = "Закон Густафсона",
                    isInline = false
                ),
                SectionContent.Table(
                    id = "1.3.6",
                    headers = listOf(
                        "Число процессоров",
                        "Закон Амдала (p=0.9)",
                        "Закон Густафсона (p=0.9)"
                    ),
                    rows = listOf(
                        listOf("2", "1.82", "1.9"),
                        listOf("4", "3.08", "3.7"),
                        listOf("8", "4.71", "7.3"),
                        listOf("16", "6.40", "14.5"),
                        listOf("32", "7.76", "29.0"),
                        listOf("64", "8.65", "58.0"),
                        listOf("128", "9.14", "116.0")
                    ),
                    caption = "Сравнение ускорения по законам Амдала и Густафсона (p=0.9)"
                ),
                SectionContent.Video(
                    id = "1.3.7",
                    url = "https://example.com/amdahl_gustafson_explanation.mp4",
                    caption = "Объяснение законов Амдала и Густафсона",
                    thumbnailUrl = "https://example.com/video_thumbnail.jpg",
                    durationSeconds = 240
                )
            )

            // Для остальных разделов можно добавить подобный контент
            else -> {
                if (sectionId == "unknown" || sectionId == "error") {
                    listOf(
                        SectionContent.Text(
                            id = "error.1",
                            content = "<p><strong>Раздел не найден</strong></p>" +
                                    "<p>К сожалению, запрошенный раздел не найден или произошла ошибка при его загрузке.</p>" +
                                    "<p>Пожалуйста, вернитесь в оглавление и выберите другой раздел для изучения.</p>"
                        )
                    )
                } else {
                    listOf(
                        SectionContent.Text(
                            id = "default.1",
                            content = "<p><strong>Содержание раздела</strong></p>" +
                                    "<p>Данный раздел находится в разработке. Скоро здесь появится полная информация.</p>" +
                                    "<p>Вы можете вернуться в оглавление и выбрать другой раздел для изучения.</p>"
                        )
                    )
                }
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