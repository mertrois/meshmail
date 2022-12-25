package app.meshmail.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MessageEntity::class, MessageFragmentEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MeshmailDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageFragmentDao(): MessageFragmentDao

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