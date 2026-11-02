package com.example.core.database

import androidx.room.TypeConverter
import java.util.Date

class DatabaseConverters {

  @TypeConverter
  fun fromTimestamp(value: Long?): Date? {
    return value?.let { Date(it) }
  }

  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? {
    return date?.time
  }

  @TypeConverter
  fun fromStringList(value: List<String>?): String? {
    return value?.joinToString(separator = ",")
  }

  @TypeConverter
  fun toStringList(value: String?): List<String>? {
    return if (value.isNullOrEmpty()) emptyList() else value.split(",")
  }
}
