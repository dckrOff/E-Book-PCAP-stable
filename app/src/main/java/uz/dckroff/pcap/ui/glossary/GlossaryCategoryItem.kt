package uz.dckroff.pcap.ui.glossary

/**
 * Модель категории глоссария для отображения в списке
 */
data class GlossaryCategoryItem(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
) 