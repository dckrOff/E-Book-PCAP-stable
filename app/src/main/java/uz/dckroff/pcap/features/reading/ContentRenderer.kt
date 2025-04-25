package uz.dckroff.pcap.features.reading

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import timber.log.Timber
import uz.dckroff.pcap.R
import uz.dckroff.pcap.data.model.DiagramElement
import uz.dckroff.pcap.data.model.SectionContent

/**
 * Класс для рендеринга различных типов контента в ReadingFragment
 */
class ContentRenderer(private val context: Context, private val container: ViewGroup) {

    /**
     * Добавляет HTML-текст
     */
    fun addHtmlText(htmlText: String, textSize: Float = 16f) {
        val textView = TextView(context).apply {
            text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)
            this.textSize = textSize
            setPadding(0, 8, 0, 8)
        }
        container.addView(textView)
    }

    /**
     * Добавляет изображение из ресурсов
     */
    fun addImageFromResource(resId: Int, description: String = "", width: Int = ViewGroup.LayoutParams.MATCH_PARENT) {
        try {
            val imageCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                layoutParams = LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8)
            }
            
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                
                // Оптимизированная загрузка изображений
                Glide.with(context)
                    .load(resId)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(800, 600)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .into(this)
            }
            
            contentLayout.addView(imageView)
            
            if (description.isNotEmpty()) {
                val descTextView = TextView(context).apply {
                    text = description
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium))
                }
                contentLayout.addView(descTextView)
            }
            
            imageCard.addView(contentLayout)
            container.addView(imageCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении изображения")
            addErrorText("Ошибка при загрузке изображения: ${e.message}")
        }
    }

    /**
     * Добавляет изображение из URL
     */
    fun addImageFromUrl(url: String, description: String = "") {
        try {
            val imageCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8)
            }
            
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                
                // Оптимизированная загрузка изображений
                Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Кэширование изображения
                    .override(800, 600) // Ограничение размера для экономии памяти
                    .format(DecodeFormat.PREFER_RGB_565) // Уменьшаем использование памяти
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.ic_error)
                    .into(this)
            }
            
            contentLayout.addView(imageView)
            
            if (description.isNotEmpty()) {
                val descTextView = TextView(context).apply {
                    text = description
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium))
                }
                contentLayout.addView(descTextView)
            }
            
            imageCard.addView(contentLayout)
            container.addView(imageCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении изображения из URL")
            addErrorText("Ошибка при загрузке изображения: ${e.message}")
        }
    }

    /**
     * Добавляет блок с кодом
     */
    fun addCodeBlock(code: String, language: String = "", showLineNumbers: Boolean = true) {
        try {
            val codeCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.code_background))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            if (language.isNotEmpty()) {
                val languageTextView = TextView(context).apply {
                    text = language.uppercase()
                    textSize = 12f
                    setPadding(16, 8, 16, 4)
                    setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium))
                }
                contentLayout.addView(languageTextView)
            }
            
            val codeTextView = TextView(context).apply {
                text = if (showLineNumbers) addLineNumbers(code) else code
                textSize = 14f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(16, 8, 16, 16)
                setTextColor(ContextCompat.getColor(context, R.color.code_text))
            }
            contentLayout.addView(codeTextView)
            
            codeCard.addView(contentLayout)
            container.addView(codeCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении блока кода")
            addErrorText("Ошибка при отображении кода: ${e.message}")
        }
    }

    /**
     * Добавляет формулу с использованием LaTeX
     */
    fun addMathFormula(formula: String, isInline: Boolean = false) {
        try {
            val webView = WebView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                settings.javaScriptEnabled = true
                
                val displayStyle = if (isInline) "" else "display"
                
                val html = """
                    <!DOCTYPE html>
                    <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <script src="https://polyfill.io/v3/polyfill.min.js?features=es6"></script>
                            <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
                            <style>
                                body {
                                    font-size: 16px;
                                    background-color: transparent;
                                    margin: 0;
                                    padding: 0;
                                    color: #212121;
                                }
                            </style>
                        </head>
                        <body>
                            <div style="text-align: center; width: 100%;">
                                \$$displayStyle
                                $formula
                                \$
                            </div>
                        </body>
                    </html>
                """.trimIndent()
                
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
            
            container.addView(webView)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении формулы")
            addErrorText("Ошибка при отображении формулы: ${e.message}")
        }
    }

    /**
     * Добавляет интерактивную схему
     */
    fun addInteractiveDiagram(imageResId: Int, description: String, interactivePoints: List<InteractivePoint>) {
        try {
            val diagramCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8)
            }
            
            // Заголовок
            val titleTextView = TextView(context).apply {
                text = description
                textSize = 16f
                setPadding(16, 16, 16, 8)
                setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_high_type))
            }
            contentLayout.addView(titleTextView)
            
            // Изображение схемы
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(context)
                    .load(imageResId)
                    .into(this)
            }
            contentLayout.addView(imageView)
            
            // Интерактивные точки
            val buttonsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 8, 16, 16)
            }
            
            interactivePoints.forEach { point ->
                val button = Button(context).apply {
                    text = point.label
                    setOnClickListener {
                        // Показываем информацию о точке
                        val infoTextView = TextView(context).apply {
                            text = point.description
                            textSize = 14f
                            setPadding(16, 8, 16, 8)
                            visibility = View.GONE
                            id = View.generateViewId()
                        }
                        
                        // Проверяем, есть ли уже информация об этой точке
                        val existingInfoId = tag as? Int
                        if (existingInfoId != null) {
                            val existingInfo = contentLayout.findViewById<TextView>(existingInfoId)
                            existingInfo?.visibility = if (existingInfo.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        } else {
                            // Добавляем описание и сохраняем его ID
                            contentLayout.addView(infoTextView, contentLayout.indexOfChild(buttonsLayout) + 1)
                            infoTextView.visibility = View.VISIBLE
                            tag = infoTextView.id
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1.0f
                    ).apply {
                        setMargins(4, 0, 4, 0)
                    }
                }
                buttonsLayout.addView(button)
            }
            
            contentLayout.addView(buttonsLayout)
            diagramCard.addView(contentLayout)
            container.addView(diagramCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении схемы")
            addErrorText("Ошибка при отображении схемы: ${e.message}")
        }
    }

    /**
     * Добавляет видео из YouTube
     */
    fun addYouTubeVideo(videoId: String, title: String = "") {
        val videoView = LayoutInflater.from(context).inflate(R.layout.item_content_video, container, false)
        
        val thumbnailView = videoView.findViewById<ImageView>(R.id.videoThumbnail)
        val titleView = videoView.findViewById<TextView>(R.id.videoTitle)
        
        // Установка заголовка
        if (title.isNotEmpty()) {
            titleView.text = title
            titleView.visibility = View.VISIBLE
        } else {
            titleView.visibility = View.GONE
        }
        
        // Загрузка превью с помощью Glide
        val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg"
        Glide.with(context)
            .load(thumbnailUrl)
            .placeholder(R.drawable.img_placeholder)
            .error(R.drawable.ic_error)
            .centerCrop()
            .into(thumbnailView)
        
        // Обработка нажатия для воспроизведения видео
        videoView.findViewById<View>(R.id.playButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            context.startActivity(intent)
        }
        
        container.addView(videoView)
    }

    /**
     * Добавляет карточку с определением
     */
    fun addDefinitionCard(term: String, definition: String) {
        try {
            val defCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.definition_background))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }
            
            val termTextView = TextView(context).apply {
                text = term
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.definition_term_color))
                setPadding(0, 0, 0, 8)
            }
            contentLayout.addView(termTextView)
            
            val defTextView = TextView(context).apply {
                text = definition
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.definition_text_color))
            }
            contentLayout.addView(defTextView)
            
            defCard.addView(contentLayout)
            container.addView(defCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении карточки определения")
            addErrorText("Ошибка при отображении определения: ${e.message}")
        }
    }

    /**
     * Добавляет интерактивный тест
     */
    fun addQuizCard(question: String, answers: List<String>, correctAnswerIndex: Int) {
        try {
            val quizCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.quiz_background))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }
            
            val questionTextView = TextView(context).apply {
                text = "Тест: $question"
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.quiz_question_color))
                setPadding(0, 0, 0, 16)
            }
            contentLayout.addView(questionTextView)
            
            val resultTextView = TextView(context).apply {
                text = ""
                textSize = 16f
                setPadding(0, 8, 0, 8)
                visibility = View.GONE
            }
            
            answers.forEachIndexed { index, answer ->
                val answerButton = Button(context).apply {
                    text = answer
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 4, 0, 4)
                    }
                    
                    setOnClickListener {
                        val isCorrect = index == correctAnswerIndex
                        resultTextView.text = if (isCorrect) {
                            "Правильно!"
                        } else {
                            "Неправильно. Правильный ответ: ${answers[correctAnswerIndex]}"
                        }
                        resultTextView.setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (isCorrect) R.color.correct_answer else R.color.wrong_answer
                            )
                        )
                        resultTextView.visibility = View.VISIBLE
                    }
                }
                contentLayout.addView(answerButton)
            }
            
            contentLayout.addView(resultTextView)
            quizCard.addView(contentLayout)
            container.addView(quizCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении теста")
            addErrorText("Ошибка при отображении теста: ${e.message}")
        }
    }

    /**
     * Добавляет выделенный текст "Важно" или "Примечание"
     */
    fun addNoteCard(text: String, type: NoteType = NoteType.NOTE) {
        try {
            val bgColor = when (type) {
                NoteType.NOTE -> R.color.note_background
                NoteType.IMPORTANT -> R.color.important_background
                NoteType.WARNING -> R.color.warning_background
            }
            
            val titleText = when (type) {
                NoteType.NOTE -> "Примечание"
                NoteType.IMPORTANT -> "Важно"
                NoteType.WARNING -> "Предупреждение"
            }
            
            val iconRes = when (type) {
                NoteType.NOTE -> R.drawable.ic_note
                NoteType.IMPORTANT -> R.drawable.ic_star
                NoteType.WARNING -> R.drawable.ic_warning
            }
            
            val noteCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }
            
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }
            
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8)
            }
            
            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 8, 0)
                }
                setImageResource(iconRes)
            }
            headerLayout.addView(iconView)
            
            val titleTextView = TextView(context).apply {
//                text = titleText
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_high_type))
            }
            headerLayout.addView(titleTextView)
            
            contentLayout.addView(headerLayout)
            
            val noteTextView = TextView(context).apply {
                this.text = text
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium))
            }
            contentLayout.addView(noteTextView)
            
            noteCard.addView(contentLayout)
            container.addView(noteCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении примечания")
            addErrorText("Ошибка при отображении примечания: ${e.message}")
        }
    }

    /**
     * Добавляет текст ошибки
     */
    fun addErrorText(errorMessage: String) {
        val errorText = TextView(context).apply {
            text = errorMessage
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        container.addView(errorText)
    }

    /**
     * Добавляет разделитель
     */
    fun addDivider() {
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.resources.getDimensionPixelSize(R.dimen.divider_height)
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        }
        container.addView(divider)
    }

    /**
     * Добавляет диаграмму с блоками и связями между ними
     */
    public fun addDiagram(content: SectionContent.Diagram) {
        try {
            val diagramCard = MaterialCardView(context).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                elevation = context.resources.getDimension(R.dimen.card_elevation)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }

            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 0, 0, 8)
            }

            // Заголовок диаграммы, если он есть
            if (content.caption.isNotEmpty()) {
                val captionView = TextView(context).apply {
                    text = content.caption
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium))
                }
                contentLayout.addView(captionView)
            }

            // Создаем кастомное представление для отрисовки диаграммы
            val diagramView = DiagramView(context, content.elements)
            diagramView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                600 // Высота диаграммы, можно рассчитать на основе элементов
            )
            contentLayout.addView(diagramView)

            diagramCard.addView(contentLayout)
            container.addView(diagramCard)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при добавлении диаграммы")
            addErrorText("Ошибка при отображении диаграммы: ${e.message}")
        }
    }

    /**
     * Кастомное представление для отрисовки диаграммы
     */
    private inner class DiagramView(
        context: Context,
        private val elements: List<DiagramElement>
    ) : View(context) {

        private val elementRects = mutableMapOf<String, RectF>()
        private val connectionPaths = mutableListOf<Path>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            // Настройка текстового отображения
            textPaint.apply {
                color = ContextCompat.getColor(context, android.R.color.black)
                textSize = 14f * resources.displayMetrics.density
                textAlign = Paint.Align.CENTER
            }

            // Настройка линий для соединений
            linePaint.apply {
                color = ContextCompat.getColor(context, android.R.color.darker_gray)
                strokeWidth = 2f * resources.displayMetrics.density
                style = Paint.Style.STROKE
            }

            // Вычисляем прямоугольники для каждого элемента
            calculateElementRects()
            // Создаем пути для соединений
            createConnectionPaths()
        }

        private fun calculateElementRects() {
            elements.forEach { element ->
                val rect = RectF(
                    element.x * resources.displayMetrics.density,
                    element.y * resources.displayMetrics.density,
                    (element.x + element.width) * resources.displayMetrics.density,
                    (element.y + element.height) * resources.displayMetrics.density
                )
                elementRects[element.id] = rect
            }
        }

        private fun createConnectionPaths() {
            elements.forEach { element ->
                element.connections.forEach { targetId ->
                    // Находим прямоугольники исходного и целевого элементов
                    val sourceRect = elementRects[element.id] ?: return@forEach
                    val targetRect = elementRects[targetId] ?: return@forEach

                    // Создаем путь между элементами
                    val path = Path()
                    val sourceX = sourceRect.centerX()
                    val sourceY = sourceRect.bottom
                    val targetX = targetRect.centerX()
                    val targetY = targetRect.top

                    path.moveTo(sourceX, sourceY)
                    path.lineTo(targetX, targetY)
                    connectionPaths.add(path)
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Отрисовка соединительных линий
            connectionPaths.forEach { path ->
                canvas.drawPath(path, linePaint)
            }

            // Отрисовка блоков и текста
            elements.forEach { element ->
                val rect = elementRects[element.id] ?: return@forEach

                // Устанавливаем цвет в зависимости от типа блока
                when (element.type) {
                    "block" -> paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
                    "decision" -> paint.color = ContextCompat.getColor(context, R.color.colorAccent)
                    else -> paint.color = ContextCompat.getColor(context, android.R.color.darker_gray)
                }

                // Рисуем блок
                paint.style = Paint.Style.FILL
                paint.alpha = 70 // Полупрозрачный фон
                canvas.drawRoundRect(rect, 8f, 8f, paint)

                // Рисуем контур
                paint.style = Paint.Style.STROKE
                paint.alpha = 255
                paint.strokeWidth = 2f
                canvas.drawRoundRect(rect, 8f, 8f, paint)

                // Рисуем текст
                canvas.drawText(
                    element.text,
                    rect.centerX(),
                    rect.centerY() + textPaint.textSize / 3, // Для выравнивания по вертикальному центру
                    textPaint
                )
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            // Определяем размеры представления
            var maxX = 0f
            var maxY = 0f

            elements.forEach { element ->
                val right = (element.x + element.width) * resources.displayMetrics.density
                val bottom = (element.y + element.height) * resources.displayMetrics.density

                if (right > maxX) maxX = right
                if (bottom > maxY) maxY = bottom
            }

            // Добавляем отступы
            maxX += 32f
            maxY += 32f

            setMeasuredDimension(
                if (maxX > 0) maxX.toInt() else widthMeasureSpec,
                if (maxY > 0) maxY.toInt() else heightMeasureSpec
            )
        }
    }

    /**
     * Вспомогательный метод для добавления номеров строк к коду
     */
    private fun addLineNumbers(code: String): String {
        val lines = code.split("\n")
        return lines.mapIndexed { index, line ->
            "${index + 1}. $line"
        }.joinToString("\n")
    }

    /**
     * Класс для интерактивных точек на схемах
     */
    data class InteractivePoint(
        val label: String,
        val description: String
    )

    /**
     * Перечисление типов заметок
     */
    enum class NoteType {
        NOTE, IMPORTANT, WARNING
    }
} 