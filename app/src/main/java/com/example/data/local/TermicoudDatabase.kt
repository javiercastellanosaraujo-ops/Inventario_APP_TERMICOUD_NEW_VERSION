package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, SaleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TermicoudDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: TermicoudDatabase? = null

        fun getDatabase(context: Context): TermicoudDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TermicoudDatabase::class.java,
                    "termicoud_inventory_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
