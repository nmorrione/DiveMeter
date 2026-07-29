package com.nmorrione.divemeter.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromDiveMethod(method: DiveMethod): String = method.name

    @TypeConverter
    fun toDiveMethod(value: String): DiveMethod = DiveMethod.valueOf(value)
}
