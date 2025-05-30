package uz.dckroff.pcap.data.local.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.dckroff.pcap.data.model.ContentItem
import java.util.Date

/**
 * Конвертеры типов для Room
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Конвертировать список разделов в строку JSON
     */
    @TypeConverter
    fun fromSectionList(sections: List<ContentItem.Section>?): String? {
        return if (sections == null) null else gson.toJson(sections)
    }

    /**
     * Конвертировать строку JSON в список разделов
     */
    @TypeConverter
    fun toSectionList(sectionsString: String?): List<ContentItem.Section>? {
        if (sectionsString == null) return null
        val type = object : TypeToken<List<ContentItem.Section>>() {}.type
        return gson.fromJson(sectionsString, type)
    }
} 