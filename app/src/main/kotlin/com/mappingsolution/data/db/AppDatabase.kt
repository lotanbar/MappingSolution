package com.mappingsolution.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mappingsolution.data.db.dao.GroupDao
import com.mappingsolution.data.db.dao.PoiDao
import com.mappingsolution.data.db.dao.RouteDao
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.db.entity.PoiEntity
import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.db.entity.RoutePointEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GroupEntity::class, PoiEntity::class, RouteEntity::class, RoutePointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun poiDao(): PoiDao
    abstract fun routeDao(): RouteDao

    companion object {
        private const val DB_NAME = "mapping_solution.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addCallback(SeedCallback())
                .fallbackToDestructiveMigration()
                .build()
    }

    /** Seeds the default "Personal POIs" group on first install. */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                db.execSQL(
                    """
                    INSERT INTO groups (name, description, iconKey, color, isVisible, createdAt, updatedAt)
                    VALUES ('Personal POIs', 'My personal points of interest', 'place', '#FF2196F3', 1,
                            ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                    """.trimIndent()
                )
            }
        }
    }
}
