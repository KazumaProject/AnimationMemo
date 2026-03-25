package com.kazumaproject.animationswipememo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoEntity::class],
    version = 2,
    exportSchema = true
)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao

    companion object {
        fun create(context: Context): MemoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MemoDatabase::class.java,
                "animation_swipe_memo.db"
            ).fallbackToDestructiveMigration()
                .build()
        }
    }
}
