package uz.dckroff.pcap

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.databinding.ActivityMainBinding
import uz.dckroff.pcap.ui.bookmarks.BookmarksFragment
import uz.dckroff.pcap.ui.dashboard.DashboardFragment
import uz.dckroff.pcap.ui.glossary.GlossaryFragment
import uz.dckroff.pcap.ui.quiz.QuizListFragment

/**
 * Главная активность приложения, содержащая ViewPager2 для переключения между основными разделами
 * и NavHostFragment для детальных экранов
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBackHandling()

        // Используем lazy initialization для View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация основной системы навигации (ViewPager + BottomNav)
        setupMainNavigation()
        
        // Инициализация детальной навигации через NavHostFragment
        setupDetailNavigation()
        
        Timber.d("MainActivity onCreate")
    }
    
    private fun setupMainNavigation() {
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
            isUserInputEnabled = false // Отключаем свайп

            // Слушатель изменения страницы для синхронизации с BottomNavigationView
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.bottomNavView.menu.getItem(position).isChecked = true
                }
            })
        }

        // Настройка обработчика нажатий на элементы нижней навигации
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboardFragment -> binding.contentMain.viewPager.currentItem = 0
                R.id.glossaryFragment -> binding.contentMain.viewPager.currentItem = 1
                R.id.quizListFragment -> binding.contentMain.viewPager.currentItem = 2
                R.id.bookmarksFragment -> binding.contentMain.viewPager.currentItem = 3
            }
            true
        }
    }
    
    private fun setupDetailNavigation() {
        // Получаем NavController из NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.mainNavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Слушатель изменений в навигации для детальных экранов
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Основные разделы (обслуживаются ViewPager)
                R.id.dashboardFragment, R.id.glossaryFragment, 
                R.id.quizListFragment, R.id.bookmarksFragment -> {
                    showMainContent()
                }
                // Любые другие экраны считаются детальными
                else -> {
                    showDetailContent()
                }
            }
        }
    }
    
    /**
     * Показать основной контент (ViewPager + BottomNav)
     */
    fun showMainContent() {
        binding.contentMain.viewPager.visibility = View.VISIBLE
        binding.bottomNavView.visibility = View.VISIBLE
        binding.contentMain.mainNavHostFragment.visibility = View.GONE
    }
    
    /**
     * Показать детальный контент (NavHostFragment)
     */
    fun showDetailContent() {
        binding.contentMain.viewPager.visibility = View.GONE
        binding.bottomNavView.visibility = View.GONE
        binding.contentMain.mainNavHostFragment.visibility = View.VISIBLE
    }
    
    /**
     * Метод для навигации к основным экранам
     */
    fun navigateToMainSection(position: Int) {
        showMainContent()
        binding.contentMain.viewPager.currentItem = position
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Выход из приложения")
            .setMessage("Вы уверены, что хотите выйти из приложения?")
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Выйти") { _, _ ->
                finish()
            }
            .show()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.contentMain.mainNavHostFragment.visibility == View.VISIBLE) {
                        if (!navController.popBackStack()) {
                            // Если в стеке навигации больше нет фрагментов, возвращаемся к основным вкладкам
                            showMainContent()
                        }
                    } else {
//                        onBackPressedDispatcher.onBackPressed()
                        showExitConfirmationDialog()
                    }
                }
            }
        )
    }

}