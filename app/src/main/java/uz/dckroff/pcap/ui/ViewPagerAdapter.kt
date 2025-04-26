package uz.dckroff.pcap.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Адаптер для ViewPager2, который управляет основными разделами приложения
 *
 * @param fragmentActivity активность, в которой размещается ViewPager2
 * @param fragments список фрагментов, которые будут отображаться в ViewPager2
 */
class ViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val fragments: List<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    /**
     * Возвращает общее количество страниц (фрагментов) в адаптере
     */
    override fun getItemCount(): Int = fragments.size

    /**
     * Создает фрагмент для указанной позиции
     *
     * @param position позиция фрагмента
     * @return фрагмент для отображения
     */
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
} 