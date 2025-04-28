package uz.dckroff.pcap.utils

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Расширения для View
 */

/**
 * Показывает View, делая его видимым
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Скрывает View, делая его невидимым, но сохраняя его место в лейауте
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Полностью скрывает View, не занимая место в лейауте
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Показывает Snackbar с сообщением об ошибке
 */
fun View.showErrorSnackbar(message: String, actionText: String = "ОК", action: (() -> Unit)? = null) {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
    
    if (action != null) {
        snackbar.setAction(actionText) { action() }
    }
    
    snackbar.show()
}

/**
 * Показывает Snackbar с сообщением об ошибке во фрагменте
 */
fun Fragment.showErrorSnackbar(message: String, actionText: String = "ОК", action: (() -> Unit)? = null) {
    view?.showErrorSnackbar(message, actionText, action)
} 