package uz.dckroff.pcap.ui.glossary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.dckroff.pcap.data.model.GlossaryTerm
import uz.dckroff.pcap.databinding.ItemGlossaryTermBinding

/**
 * Адаптер для отображения терминов глоссария
 */
class GlossaryTermsAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<GlossaryTerm, GlossaryTermsAdapter.TermViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        val binding = ItemGlossaryTermBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TermViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TermViewHolder(
        private val binding: ItemGlossaryTermBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).id)
                }
            }
        }

        fun bind(term: GlossaryTerm) {
            binding.textTerm.text = term.term
            binding.textCategory.text = term.category
            binding.textDefinition.text = term.definition
                .take(100) // Ограничиваем длину определения для краткости
                .let { if (term.definition.length > 100) "$it..." else it }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GlossaryTerm>() {
            override fun areItemsTheSame(oldItem: GlossaryTerm, newItem: GlossaryTerm): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: GlossaryTerm, newItem: GlossaryTerm): Boolean {
                return oldItem == newItem
            }
        }
    }
} 