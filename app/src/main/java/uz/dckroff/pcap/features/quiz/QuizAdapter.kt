package uz.dckroff.pcap.features.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.data.model.Quiz
import uz.dckroff.pcap.databinding.ItemQuizBinding

/**
 * Адаптер для отображения списка тестов
 */
class QuizAdapter(
    private val onQuizClicked: (Quiz) -> Unit,
    private val onResultsClicked: (Quiz) -> Unit
) : ListAdapter<Quiz, QuizAdapter.QuizViewHolder>(QuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val binding = ItemQuizBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuizViewHolder(
        private val binding: ItemQuizBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.startButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onQuizClicked(getItem(position))
                }
            }

            binding.resultButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onResultsClicked(getItem(position))
                }
            }
        }

        fun bind(quiz: Quiz) {
            with(binding) {
                // Заголовок и описание теста
                titleTextView.text = quiz.title
                descriptionTextView.text = quiz.description

                // Чип сложности
                difficultyChip.text = quiz.difficulty.getLocalizedName()
                difficultyChip.chipBackgroundColor = ContextCompat.getColorStateList(
                    root.context,
                    quiz.difficulty.getColorRes()
                )

                // Информация о тесте
                questionsCountTextView.text = "${quiz.questionsCount} вопросов"
                timeLimitTextView.text = "${quiz.timeLimit} минут"

                // Показываем результат, если тест выполнен
                if (quiz.isCompleted) {
                    scoreTextView.text = "${quiz.lastScore}%"
                    scoreTextView.visibility = View.VISIBLE
                    
                    // Показываем кнопку "Результаты"
                    resultButton.visibility = View.VISIBLE
                    
                    // Меняем текст кнопки старта
                    startButton.text = "Пройти снова"
                } else {
                    scoreTextView.visibility = View.GONE
                    resultButton.visibility = View.GONE
                    startButton.text = "Начать"
                }
            }
        }
    }

    /**
     * DiffUtil.Callback для эффективного обновления списка
     */
    class QuizDiffCallback : DiffUtil.ItemCallback<Quiz>() {
        override fun areItemsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem == newItem
        }
    }
} 