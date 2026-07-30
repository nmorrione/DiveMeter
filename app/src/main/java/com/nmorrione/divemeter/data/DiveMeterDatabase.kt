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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dives ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE dives ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dives ADD COLUMN ownerNickname TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Dive::class], version = 4, exportSchema = false)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
