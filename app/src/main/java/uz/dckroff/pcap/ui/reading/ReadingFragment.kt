package uz.dckroff.pcap.ui.reading

import android.content.Intent
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.bumptech.glide.Glide
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

    // Сохраняем ссылку на слушатель для возможности его удаления
    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null

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
        setupScrollListener()

        // Показываем прогресс перед загрузкой
        binding.progressBar.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        binding.fabNextSection.visibility = View.GONE // Изначально скрываем кнопку

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

        // Наблюдаем за разделами, чтобы обновить видимость кнопки
        viewModel.allSections.observe(viewLifecycleOwner) { sections ->
            updateNextButtonVisibility()
        }

        // Наблюдаем за сохраненной позицией прокрутки
        viewModel.savedScrollPosition.observe(viewLifecycleOwner) { position ->
            if (position > 0) {
                binding.nestedScrollView.post {
                    binding.nestedScrollView.scrollTo(0, position)
                    Timber.d("Восстановлена позиция прокрутки: $position")
                }
            }
        }

        // Наблюдаем за прогрессом чтения
        viewModel.readingProgress.observe(viewLifecycleOwner) { progress ->
            // Обновляем индикатор прогресса
            binding.progressIndicator.setProgress(progress, true)
            Timber.d("Прогресс чтения обновлен: $progress%")
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

        // Всегда показываем кнопку
        binding.fabNextSection.visibility = View.VISIBLE

        // Если есть следующий раздел, кнопка ведет к нему, иначе - возврат на главный экран
        if (nextSection != null) {
            // Для следующего раздела используем стрелку вправо
            binding.fabNextSection.setImageResource(R.drawable.ic_arrow_forward)
            binding.fabNextSection.contentDescription = getString(R.string.next_section)
            Timber.d("Кнопка 'Далее' - переход к разделу: ${nextSection.title}")
        } else {
            // Для возврата на главный экран используем иконку домой
            binding.fabNextSection.setImageResource(R.drawable.ic_home)
            binding.fabNextSection.contentDescription = getString(R.string.return_to_home)
            Timber.d("Кнопка 'Далее' - возврат на главный экран")
        }
    }

    /**
     * Настройка слушателя прокрутки для отслеживания прогресса чтения
     */
    private fun setupScrollListener() {
        scrollListener = ViewTreeObserver.OnScrollChangedListener {
            if (_binding != null) { // Проверяем, что binding еще существует
                val scrollView = binding.nestedScrollView
                val scrollPosition = scrollView.scrollY

                // Вычисляем прогресс прокрутки (0-100%)
                val scrollRange = scrollView.getChildAt(0).height - scrollView.height
                val scrollPercentage = if (scrollRange > 0) {
                    (scrollPosition * 100) / scrollRange
                } else {
                    0
                }

                Timber.d("Прокрутка: позиция $scrollPosition, прогресс $scrollPercentage%")

                // Сохраняем позицию прокрутки
                chapterId?.let { chId ->
                    viewModel.saveReadingPosition(chId, currentSectionId, scrollPosition)
                }

                // Если прокручено больше 75%, считаем раздел прочитанным
                if (scrollPercentage > 75) {
                    viewModel.updateReadingProgress(currentSectionId, 100)
                } else {
                    // Иначе устанавливаем прогресс равный проценту прокрутки
                    viewModel.updateReadingProgress(currentSectionId, scrollPercentage)
                }
            }
        }

        // Добавляем слушатель к ViewTreeObserver
        binding.nestedScrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    /**
     * Переход к следующему разделу или возврат на главный экран
     */
    private fun navigateToNextSection() {
        val nextSection = viewModel.getNextSection(currentSectionId)

        // Отмечаем текущий раздел как прочитанный перед переходом к следующему
        viewModel.markSectionAsRead(currentSectionId)

        // Добавляем небольшую задержку, чтобы база данных обновилась
        binding.root.postDelayed({
            if (nextSection != null) {
                // Переходим к следующему разделу
                try {
                    Timber.d("Переход к следующему разделу: ${nextSection.id}")

                    // Переход к следующему разделу в Navigation Component
                    findNavController().navigate(
                        R.id.actionReadingFragmentSelf,
                        Bundle().apply {
                            putString("chapterId", nextSection.chapterId)
                            putString("sectionId", nextSection.id)
                            putString("sectionTitle", nextSection.title)
                        }
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при переходе к следующему разделу")
                }
            } else {
                // Возврат на главный экран (Dashboard)
                Timber.d("Возврат на главный экран (Dashboard)")
                try {
                    // Явный переход на Dashboard вместо простого navigateUp()
                    findNavController().navigate(R.id.dashboardFragment)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.section_completed),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при возврате на главный экран")
                    // Запасной вариант - просто вернуться назад
                    findNavController().navigateUp()
                }
            }
        }, 300) // Небольшая задержка перед переходом
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

            // После добавления контента обновляем видимость кнопки
            updateNextButtonVisibility()

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

            // Скрываем кнопку навигации в случае ошибки
            binding.fabNextSection.visibility = View.GONE
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }

            // Выделение текста, если нужно
            if (content.isHighlighted) {
                setBackgroundColor(resources.getColor(R.color.text_color_medium, null))
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

//        // Загрузка изображения (использовать Glide или другую библиотеку)
//        // В демо-версии можно использовать плейсхолдер
//        imageView.setImageResource(R.drawable.img_placeholder)

        Glide
            .with(requireContext())
            .load(content.url)
            .centerCrop()
            .placeholder(R.drawable.img)
            .into(imageView)

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
        codeView.typeface = android.graphics.Typeface.MONOSPACE

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

        // Получаем ID видео из URL
        val videoId = extractYouTubeId(content.url)

        // Загружаем обложку видео если это YouTube видео
        if (videoId != null) {
            // Формируем URL обложки в высоком качестве
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            Log.e("TAG", "addVideoContent: thumbnailUrl: " + thumbnailUrl)

            // Загружаем превью с помощью Glide
            Glide.with(requireContext())
                .load(thumbnailUrl)
                .placeholder(R.drawable.img)
                .error(R.drawable.img)
                .centerCrop()
                .into(thumbnailView)
        } else {
            // Если не удалось получить ID видео или это не YouTube видео, используем плейсхолдер
            thumbnailView.setImageResource(R.drawable.img_placeholder)
        }

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
            // Получаем URL видео из YouTube
            val videoUrl = content.url

            try {
                // Создаем Intent для открытия видео
                val intent = Intent(Intent.ACTION_VIEW)

                // Проверяем, является ли URL ссылкой на YouTube
                if (videoUrl.contains("youtu.be") || videoUrl.contains("youtube.com")) {
                    // Устанавливаем URI YouTube видео
                    intent.data = Uri.parse(videoUrl)

                    // Пытаемся открыть в приложении YouTube если оно установлено
                    intent.setPackage("com.google.android.youtube")

                    // Проверяем, есть ли приложение, которое может обработать этот Intent
                    if (intent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        // Если приложение YouTube не установлено, открываем в браузере
                        intent.setPackage(null) // Сбрасываем package для открытия в любом браузере
                        startActivity(intent)
                    }
                } else {
                    // Для других типов видео можно использовать встроенный видеоплеер
                    Toast.makeText(
                        requireContext(),
                        "Неподдерживаемый формат видео: ${content.url}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при попытке воспроизведения видео")
                Toast.makeText(
                    requireContext(),
                    "Ошибка при воспроизведении видео: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
     * Извлекает ID видео из YouTube URL
     * Поддерживает форматы:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     */
    private fun extractYouTubeId(url: String): String? {
        val regex = Regex(
            pattern = "(?:youtube\\.com.*[?&]v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})",
            options = setOf(RegexOption.IGNORE_CASE)
        )
        val match = regex.find(url)
        return match?.groups?.get(1)?.value
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
                setTypeface(null, android.graphics.Typeface.BOLD)
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

    override fun onPause() {
        super.onPause()

        // Сохраняем позицию чтения при уходе с экрана
        if (_binding != null) {
            val scrollPosition = binding.nestedScrollView.scrollY
            chapterId?.let { chId ->
                viewModel.saveReadingPosition(chId, currentSectionId, scrollPosition)
            }
        }
    }

    override fun onDestroyView() {
        // Удаляем слушатель прокрутки перед уничтожением представления
        try {
            scrollListener?.let { listener ->
                binding.nestedScrollView.viewTreeObserver.removeOnScrollChangedListener(listener)
                scrollListener = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при удалении слушателя прокрутки")
        }

        super.onDestroyView()
        _binding = null
    }
} 