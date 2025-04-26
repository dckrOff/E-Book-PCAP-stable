package uz.dckroff.pcap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.databinding.ActivityMainBinding
import uz.dckroff.pcap.features.bookmarks.BookmarksFragment
import uz.dckroff.pcap.features.dashboard.DashboardFragment
import uz.dckroff.pcap.features.glossary.GlossaryFragment
import uz.dckroff.pcap.features.quiz.QuizListFragment
import uz.dckroff.pcap.ui.ViewPagerAdapter

/**
 * Главная активность приложения, содержащая ViewPager2 для переключения между основными разделами
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Используем lazy initialization для View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // Отключаем анимацию при отрисовке View для ускорения
//        ViewCompat.setLayoutDirection(binding.root, ViewCompat.LAYOUT_DIRECTION_LTR)

        // Создаем список фрагментов для ViewPager
        val fragments = listOf(
            DashboardFragment(),
            GlossaryFragment(),
            QuizListFragment(),
            BookmarksFragment()
        )

        // Инициализируем адаптер и настраиваем ViewPager2
        viewPagerAdapter = ViewPagerAdapter(this, fragments)
        binding.contentMain.viewPager.apply {
            adapter = viewPagerAdapter
            offscreenPageLimit = fragments.size // Предварительно загружаем все страницы
            isUserInputEnabled = false // Разрешаем свайп

            // Слушатель изменения страницы для синхронизации с BottomNavigationView
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.bottomNavView.menu.getItem(position).isChecked = true
                }
            })
        }

        // Настройка обработчика нажатий на элементы нижней навигации
        binding.bottomNavView.setOnItemSelectedListener { item ->
//            binding.contentMain.viewPager.setCurrentItem(item, false)
            when (item.itemId) {
                R.id.dashboardFragment -> binding.contentMain.viewPager.currentItem = 0
                R.id.glossaryFragment -> binding.contentMain.viewPager.currentItem = 1
                R.id.quizListFragment -> binding.contentMain.viewPager.currentItem = 2
                R.id.bookmarksFragment -> binding.contentMain.viewPager.currentItem = 3
            }
            true
        }

        Timber.d("MainActivity onCreate")
    }
}