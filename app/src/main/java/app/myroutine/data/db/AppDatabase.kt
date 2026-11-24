package app.myroutine.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 스키마 변경 시: version 올리고 Migration 추가 (app/schemas/ 에 JSON 이 export 됨).
 * 자동 마이그레이션이 가능한 변경이면 autoMigrations 에 AutoMigration(from, to) 추가.
 */
@Database(
    entities = [
        RoutineEntity::class,
        RepeatRuleEntity::class,
        RoutineLogEntity::class,
        TagEntity::class,
        RoutineTagEntity::class,
        TodoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun repeatRuleDao(): RepeatRuleDao
    abstract fun routineLogDao(): RoutineLogDao
    abstract fun tagDao(): TagDao
    abstract fun routineTagDao(): RoutineTagDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val NAME = "myroutine.db"
    }
}
