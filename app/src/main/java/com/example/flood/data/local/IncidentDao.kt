package com.example.flood.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flood.data.model.Incident
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<Incident>>

    @Query("SELECT * FROM incidents WHERE type = :type ORDER BY createdAt DESC")
    fun getIncidentsByType(type: String): Flow<List<Incident>>

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: Incident): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<Incident>)

    @Update
    suspend fun updateIncident(incident: Incident)

    @Delete
    suspend fun deleteIncident(incident: Incident)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun clearAll()
}
