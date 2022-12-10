package app.meshmail.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TestEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDao(): TestDao
}