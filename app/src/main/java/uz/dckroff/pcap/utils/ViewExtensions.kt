package uz.dckroff.pcap.utils

import android.view.View

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