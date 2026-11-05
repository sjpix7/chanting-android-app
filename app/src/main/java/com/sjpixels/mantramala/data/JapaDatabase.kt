package com.sjpixels.mantramala.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
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

/** How a mala was counted: tapped while chanting, or played from a voice sample. */
object MalaSource {
    const val TAP = "tap"
    const val VOICE = "voice"
}

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
    /** [MalaSource.TAP] or [MalaSource.VOICE]. */
    @ColumnInfo(defaultValue = MalaSource.TAP) val source: String = MalaSource.TAP,
) {
    val isVoice: Boolean get() = source == MalaSource.VOICE
}

@Entity(tableName = "custom_mantras")
data class CustomMantra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val text: String,
    val meaning: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A voice recording of a mantra, recorded in the app or imported from a file. Audio lives in filesDir/voice/. */
@Entity(tableName = "voice_samples", indices = [Index("mantraKey")])
data class VoiceSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mantraKey: String,
    val name: String,
    /** File name inside filesDir/voice/. */
    val fileName: String,
    val durationMs: Long,
    val recorded: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    /** The sample picked most recently is the one that plays for its mantra. */
    val selectedAt: Long = createdAt,
)

@Dao
interface VoiceSampleDao {
    @Insert
    suspend fun insert(sample: VoiceSample): Long

    @Query("SELECT * FROM voice_samples ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<VoiceSample>>

    @Query("SELECT * FROM voice_samples WHERE id = :id")
    suspend fun getById(id: Long): VoiceSample?

    @Query("SELECT * FROM voice_samples WHERE mantraKey = :mantraKey")
    suspend fun forMantra(mantraKey: String): List<VoiceSample>

    @Query("UPDATE voice_samples SET selectedAt = :at WHERE id = :id")
    suspend fun select(id: Long, at: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(sample: VoiceSample)
}

@Dao
interface JapaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: JapaRecord): Long

    @Query("SELECT * FROM japa_records ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<JapaRecord>>

    @Delete
    suspend fun delete(record: JapaRecord)

    @Query("DELETE FROM japa_records WHERE dayKey = :dayKey AND source IN (:sources)")
    suspend fun deleteDay(dayKey: Int, sources: List<String>)

    @Query("DELETE FROM japa_records WHERE source IN (:sources)")
    suspend fun deleteAll(sources: List<String>)

    /** Deletes every mala completed before [dayKey] (yyyyMMdd); that day and later are kept. */
    @Query("DELETE FROM japa_records WHERE dayKey < :dayKey AND source IN (:sources)")
    suspend fun deleteBefore(dayKey: Int, sources: List<String>)
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

@Database(
    entities = [JapaRecord::class, CustomMantra::class, VoiceSample::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class JapaDatabase : RoomDatabase() {
    abstract fun japaDao(): JapaDao
    abstract fun customMantraDao(): CustomMantraDao
    abstract fun voiceSampleDao(): VoiceSampleDao

    companion object {
        fun create(context: Context): JapaDatabase =
            Room.databaseBuilder(context, JapaDatabase::class.java, "mantra_mala.db").build()
    }
}
