package app.meshmail.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TestEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDao(): TestDao
}