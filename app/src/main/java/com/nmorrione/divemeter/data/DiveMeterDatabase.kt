package com.nmorrione.divemeter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Dive::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DiveMeterDatabase : RoomDatabase() {
    abstract fun diveDao(): DiveDao

    companion object {
        @Volatile
        private var instance: DiveMeterDatabase? = null

        fun getInstance(context: Context): DiveMeterDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiveMeterDatabase::class.java,
                    "divemeter.db"
                ).build().also { instance = it }
            }
    }
}
