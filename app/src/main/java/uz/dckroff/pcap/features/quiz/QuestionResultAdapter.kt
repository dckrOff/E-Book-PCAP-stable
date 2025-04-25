package uz.dckroff.pcap.features.quiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.R
import uz.dckroff.pcap.databinding.ItemQuestionResultBinding

/**
 * Адаптер для отображения результатов вопросов в тесте
 */
class QuestionResultAdapter : ListAdapter<QuizResultsViewModel.QuestionWithAnswer, QuestionResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuestionResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemQuestionResultBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: QuizResultsViewModel.QuestionWithAnswer) {
            with(binding) {
                // Номер вопроса
                questionNumberTextView.text = "Вопрос ${item.number}"
                
                // Текст вопроса
                questionTextView.text = item.question.text
                
                // Правильный ответ
                correctAnswerTextView.text = item.getCorrectAnswerText()
                
                // Ответ пользователя
                yourAnswerTextView.text = item.getUserAnswerText()
                
                // Иконка результата (правильно/неправильно)
                val isCorrect = item.isCorrect()
                val context = root.context
                
                if (isCorrect) {
                    resultIconImageView.setImageResource(R.drawable.ic_correct)
                    resultIconImageView.setColorFilter(
                        ContextCompat.getColor(context, R.color.text_color_easy)
                    )
                    yourAnswerTextView.setTextColor(
                        ContextCompat.getColor(context, R.color.text_color_easy)
                    )
                } else {
                    resultIconImageView.setImageResource(R.drawable.ic_incorrect)
                    resultIconImageView.setColorFilter(
                        ContextCompat.getColor(context, R.color.text_color_hard)
                    )
                    yourAnswerTextView.setTextColor(
                        ContextCompat.getColor(context, R.color.text_color_hard)
                    )
                }
                
                // Объяснение (если есть)
                if (item.question.explanation != null) {
                    explanationLabelTextView.visibility = View.VISIBLE
                    explanationTextView.visibility = View.VISIBLE
                    explanationTextView.text = item.question.explanation
                } else {
                    explanationLabelTextView.visibility = View.GONE
                    explanationTextView.visibility = View.GONE
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<QuizResultsViewModel.QuestionWithAnswer>() {
        override fun areItemsTheSame(
            oldItem: QuizResultsViewModel.QuestionWithAnswer,
            newItem: QuizResultsViewModel.QuestionWithAnswer
        ): Boolean {
            return oldItem.question.id == newItem.question.id
        }

        override fun areContentsTheSame(
            oldItem: QuizResultsViewModel.QuestionWithAnswer,
            newItem: QuizResultsViewModel.QuestionWithAnswer
        ): Boolean {
            return oldItem == newItem
        }
    }
} 