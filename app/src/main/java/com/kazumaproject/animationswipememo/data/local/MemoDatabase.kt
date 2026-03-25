package com.kazumaproject.animationswipememo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MemoEntity::class, SavedDrawingEntity::class],
    version = 4,
    exportSchema = true
)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun savedDrawingDao(): SavedDrawingDao

    companion object {
        fun create(context: Context): MemoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MemoDatabase::class.java,
                "animation_swipe_memo.db"
            )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE memos ADD COLUMN title TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
