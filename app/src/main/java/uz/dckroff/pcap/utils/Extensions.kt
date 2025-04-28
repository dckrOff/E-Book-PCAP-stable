package uz.dckroff.pcap.utils

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Отображает Snackbar с сообщением об ошибке
 * @param message сообщение об ошибке
 * @param actionText текст кнопки действия (по умолчанию "OK")
 * @param action действие, которое будет выполнено при нажатии на кнопку
 */
//fun Fragment.showErrorSnackbar(
//    message: String,
//    actionText: String = "OK",
//    action: (() -> Unit)? = null
//) {
//    view?.let { rootView ->
//        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
//
//        action?.let {
//            snackbar.setAction(actionText) { it() }
//        }
//
//        snackbar.show()
//    }
//}

/**
 * Отображает Snackbar с сообщением
 * @param message сообщение
 * @param duration длительность отображения (по умолчанию Snackbar.LENGTH_SHORT)
 */
fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT
) {
    view?.let { rootView ->
        Snackbar.make(rootView, message, duration).show()
    }
}

/**
 * Отображает Snackbar с действием
 * @param message сообщение
 * @param actionText текст кнопки действия
 * @param action действие при нажатии на кнопку
 * @param duration длительность отображения (по умолчанию Snackbar.LENGTH_LONG)
 */
fun Fragment.showActionSnackbar(
    message: String,
    actionText: String,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    view?.let { rootView ->
        Snackbar.make(rootView, message, duration)
            .setAction(actionText) { action() }
            .show()
    }
}

/**
 * Отображает/скрывает View
 * @param isVisible если true, View будет отображен, иначе скрыт
 */
fun View.setVisible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

/**
 * Включает/отключает View
 * @param isEnabled если true, View будет активен, иначе неактивен
 */
fun View.setEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
    alpha = if (isEnabled) 1.0f else 0.5f
} 