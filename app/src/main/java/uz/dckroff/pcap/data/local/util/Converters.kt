package uz.dckroff.pcap.data.local.util

import androidx.room.TypeConverter
import java.util.Date

/**
 * Конвертеры типов для Room
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 