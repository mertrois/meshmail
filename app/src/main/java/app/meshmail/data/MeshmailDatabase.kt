package app.meshmail.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TestEntity::class, MessageEntity::class, MessageFragmentEntity::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class MeshmailDatabase : RoomDatabase() {
    abstract fun testDao(): TestDao
    abstract fun messageDao(): MessageDao
    abstract fun messageFragmentDao(): MessageFragmentDao

    // syntactic sugar, currently broken when called from service.
    companion object {
        fun getDatabase(context: Context): MeshmailDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MeshmailDatabase::class.java,
                "meshmail_database"
            ).fallbackToDestructiveMigration().build()
        }
    }
}