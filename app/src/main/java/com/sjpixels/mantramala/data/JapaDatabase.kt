package com.sjpixels.mantramala.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One completed mala (108 beads). */
@Entity(tableName = "japa_records", indices = [Index("dayKey")])
data class JapaRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Built-in mantra id, or "custom:<id>" for a user mantra. */
    val mantraKey: String,
    /** Name snapshot, so history still reads correctly if a custom mantra is deleted. */
    val mantraLabel: String,
    val beads: Int = MALA_SIZE,
    /** When the first bead of this round was counted (0 if unknown). */
    val startedAt: Long,
    val completedAt: Long,
    /** Local calendar day of completion, yyyyMMdd. */
    val dayKey: Int,
)

@Entity(tableName = "custom_mantras")
data class CustomMantra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val text: String,
    val meaning: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface JapaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: JapaRecord): Long

    @Query("SELECT * FROM japa_records ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<JapaRecord>>

    @Delete
    suspend fun delete(record: JapaRecord)

    @Query("DELETE FROM japa_records WHERE dayKey = :dayKey")
    suspend fun deleteDay(dayKey: Int)

    @Query("DELETE FROM japa_records")
    suspend fun deleteAll()

    /** Deletes every mala completed before [dayKey] (yyyyMMdd); that day and later are kept. */
    @Query("DELETE FROM japa_records WHERE dayKey < :dayKey")
    suspend fun deleteBefore(dayKey: Int)
}

@Dao
interface CustomMantraDao {
    @Insert
    suspend fun insert(mantra: CustomMantra): Long

    @Query("SELECT * FROM custom_mantras WHERE id = :id")
    suspend fun getById(id: Long): CustomMantra?

    @Query("SELECT * FROM custom_mantras ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomMantra>>

    @Delete
    suspend fun delete(mantra: CustomMantra)
}

@Database(entities = [JapaRecord::class, CustomMantra::class], version = 1, exportSchema = true)
abstract class JapaDatabase : RoomDatabase() {
    abstract fun japaDao(): JapaDao
    abstract fun customMantraDao(): CustomMantraDao

    companion object {
        fun create(context: Context): JapaDatabase =
            Room.databaseBuilder(context, JapaDatabase::class.java, "mantra_mala.db").build()
    }
}
