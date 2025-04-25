package uz.dckroff.pcap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import uz.dckroff.pcap.databinding.ActivityMainBinding
import uz.dckroff.pcap.features.bookmarks.BookmarksFragment
import uz.dckroff.pcap.features.dashboard.DashboardFragment
import uz.dckroff.pcap.features.glossary.GlossaryFragment
import uz.dckroff.pcap.features.quiz.QuizListFragment
import uz.dckroff.pcap.features.settings.SettingsFragment

/**
 * Главная активность приложения, содержащая Navigation Component
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    // Кеш для фрагментов, чтобы избежать повторного создания
    private val fragmentsCache = mutableMapOf<Int, Fragment>()
    
    // Список идентификаторов пунктов нижней навигации
    private val bottomNavDestinations = listOf(
        R.id.dashboardFragment,
        R.id.contentListFragment,
        R.id.glossaryFragment,
        R.id.quizListFragment,
        R.id.settingsFragment,
        R.id.bookmarksFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Используем lazy initialization для View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Отключаем анимацию при отрисовке View для ускорения
        ViewCompat.setLayoutDirection(binding.root, ViewCompat.LAYOUT_DIRECTION_LTR)

        // Инициализация FragmentManager для управления фрагментами
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController
        
        // Настройка нижней панели навигации
        binding.bottomNavView.setupWithNavController(navController)
        
        // Настройка слушателя нижней навигации для оптимизации переключения между фрагментами
        binding.bottomNavView.setOnItemSelectedListener { item ->
            val destinationId = item.itemId
            
            // Проверяем, поддерживается ли этот пункт назначения для оптимизации
            if (destinationId in bottomNavDestinations) {
                // Проверяем, не является ли это текущим фрагментом
                if (navController.currentDestination?.id != destinationId) {
                    // Переходим к пункту назначения без пересоздания, если возможно
                    navController.navigate(destinationId)
                }
                true
            } else {
                // Для других пунктов используем стандартную навигацию
                navController.navigate(destinationId)
                true
            }
        }
        
        // Слушатель для оптимизации навигации и повторного использования фрагментов
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in bottomNavDestinations) {
                // Очистка стека назад для пунктов нижней навигации
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                val fragmentManager = navHostFragment.childFragmentManager
                
                // Устанавливаем состояние RESUMED для текущего фрагмента и STARTED для остальных
                fragmentManager.fragments.forEach { fragment ->
                    if (fragment.id == destination.id) {
                        fragmentManager.beginTransaction()
                            .setMaxLifecycle(fragment, androidx.lifecycle.Lifecycle.State.RESUMED)
                            .commit()
                    } else if (fragment.id in bottomNavDestinations) {
                        fragmentManager.beginTransaction()
                            .setMaxLifecycle(fragment, androidx.lifecycle.Lifecycle.State.STARTED)
                            .commit()
                    }
                }
            }
        }
        
        Timber.d("MainActivity onCreate")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}