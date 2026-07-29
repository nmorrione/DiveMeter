package com.nmorrione.divemeter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dives ADD COLUMN videoUri TEXT")
    }
}

@Database(entities = [Dive::class], version = 2, exportSchema = false)
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
