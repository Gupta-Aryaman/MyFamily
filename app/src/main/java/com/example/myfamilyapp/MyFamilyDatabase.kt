package com.example.myfamilyapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [ContactModel::class], version = 1, exportSchema = false)
public abstract class MyFamilyDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    // similar to static in java
    companion object {

        @Volatile
        private var INSTANCE: MyFamilyDatabase? = null

        fun getDatabase(context: Context): MyFamilyDatabase {

            INSTANCE?.let {
                return it
            }

            return synchronized(MyFamilyDatabase::class.java) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyFamilyDatabase::class.java,
                    "my_family_db"
                ).build()

                INSTANCE = instance

                instance
            }
        }

    }


}