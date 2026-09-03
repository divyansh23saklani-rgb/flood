package com.example.flood.data.repository

import com.example.flood.data.local.CommentDao
import com.example.flood.data.local.IncidentDao
import com.example.flood.data.local.SeedData
import com.example.flood.data.model.Comment
import com.example.flood.data.model.Incident
import kotlinx.coroutines.flow.Flow

class IncidentRepository(
    private val dao: IncidentDao,
    private val commentDao: CommentDao
) {

    val allIncidents: Flow<List<Incident>> = dao.getAllIncidents()
    val allComments: Flow<List<Comment>> = commentDao.getAllComments()

    fun getIncidentsByType(type: String): Flow<List<Incident>> = dao.getIncidentsByType(type)

    fun getCommentsForIncident(incidentId: Long, incidentCreatedAt: Long): Flow<List<Comment>> =
        commentDao.getCommentsForIncident(incidentId, incidentCreatedAt)

    suspend fun getIncidentById(id: Long): Incident? = dao.getIncidentById(id)

    suspend fun getIncidentByCreatedAt(createdAt: Long): Incident? = dao.getIncidentByCreatedAt(createdAt)

    suspend fun addIncident(
        type: String,
        note: String,
        lat: Double,
        lng: Double,
        severity: String = "HIGH",
        createdAt: Long = System.currentTimeMillis()
    ): Incident {
        val incident = Incident(
            type = type,
            note = note,
            lat = lat,
            lng = lng,
            createdAt = createdAt,
            severity = severity,
            userReported = true,
            score = 0,
            upvotes = 0,
            downvotes = 0,
            status = "OPEN",
            userVote = 0,
            isAlertBroadcasted = false
        )
        val id = dao.insertIncident(incident)
        return incident.copy(id = id)
    }

    suspend fun insertRemoteIncident(incident: Incident): Boolean {
        val count = dao.checkExists(incident.createdAt)
        if (count == 0) {
            dao.insertIncident(incident)
            return true
        }
        return false
    }

    suspend fun markAlertBroadcasted(id: Long) {
        dao.updateAlertBroadcasted(id, true)
    }

    suspend fun markAlertBroadcastedByCreatedAt(createdAt: Long) {
        dao.updateAlertBroadcastedByCreatedAt(createdAt, true)
    }

    suspend fun toggleUpvote(id: Long): Pair<Incident?, Boolean> {
        val incident = dao.getIncidentById(id) ?: return Pair(null, false)
        val currentVote = incident.userVote
        val newVote: Int
        var newUpvotes = incident.upvotes
        var newDownvotes = incident.downvotes

        if (currentVote == 1) {
            // Remove upvote
            newVote = 0
            newUpvotes = (newUpvotes - 1).coerceAtLeast(0)
        } else {
            if (currentVote == -1) {
                newDownvotes = (newDownvotes - 1).coerceAtLeast(0)
            }
            newVote = 1
            newUpvotes += 1
        }
        val newScore = newUpvotes - newDownvotes
        dao.updateVotes(id, newUpvotes, newDownvotes, newScore, newVote)

        // Threshold reached if newUpvotes >= 3 and not already broadcasted
        val justReachedThreshold = newUpvotes >= 3 && !incident.isAlertBroadcasted
        if (justReachedThreshold) {
            dao.updateAlertBroadcasted(id, true)
        }

        val updated = incident.copy(
            upvotes = newUpvotes,
            downvotes = newDownvotes,
            score = newScore,
            userVote = newVote,
            isAlertBroadcasted = if (justReachedThreshold) true else incident.isAlertBroadcasted
        )
        return Pair(updated, justReachedThreshold)
    }

    suspend fun toggleDownvote(id: Long): Incident? {
        val incident = dao.getIncidentById(id) ?: return null
        val currentVote = incident.userVote
        val newVote: Int
        var newUpvotes = incident.upvotes
        var newDownvotes = incident.downvotes

        if (currentVote == -1) {
            // Remove downvote
            newVote = 0
            newDownvotes = (newDownvotes - 1).coerceAtLeast(0)
        } else {
            if (currentVote == 1) {
                newUpvotes = (newUpvotes - 1).coerceAtLeast(0)
            }
            newVote = -1
            newDownvotes += 1
        }
        val newScore = newUpvotes - newDownvotes
        dao.updateVotes(id, newUpvotes, newDownvotes, newScore, newVote)
        return incident.copy(upvotes = newUpvotes, downvotes = newDownvotes, score = newScore, userVote = newVote)
    }

    suspend fun updateIncidentStatus(id: Long, newStatus: String): Incident? {
        val incident = dao.getIncidentById(id) ?: return null
        dao.updateStatus(id, newStatus)
        return incident.copy(status = newStatus)
    }

    suspend fun applyRemoteVote(createdAt: Long, upvotes: Int, downvotes: Int, score: Int): Pair<Incident?, Boolean> {
        val existing = dao.getIncidentByCreatedAt(createdAt)
        dao.updateVotesByCreatedAt(createdAt, upvotes, downvotes, score)
        if (existing != null) {
            val justReachedThreshold = upvotes >= 3 && !existing.isAlertBroadcasted
            if (justReachedThreshold) {
                dao.updateAlertBroadcastedByCreatedAt(createdAt, true)
            }
            val updated = existing.copy(
                upvotes = upvotes,
                downvotes = downvotes,
                score = score,
                isAlertBroadcasted = if (justReachedThreshold) true else existing.isAlertBroadcasted
            )
            return Pair(updated, justReachedThreshold)
        }
        return Pair(null, false)
    }

    suspend fun applyRemoteStatus(createdAt: Long, status: String) {
        dao.updateStatusByCreatedAt(createdAt, status)
    }

    suspend fun addComment(
        incidentId: Long,
        incidentCreatedAt: Long,
        authorName: String,
        text: String,
        senderDeviceId: String
    ): Comment {
        val comment = Comment(
            incidentId = incidentId,
            incidentCreatedAt = incidentCreatedAt,
            authorName = authorName.ifBlank { "Community Member" },
            text = text,
            createdAt = System.currentTimeMillis(),
            senderDeviceId = senderDeviceId
        )
        val id = commentDao.insertComment(comment)
        return comment.copy(id = id)
    }

    suspend fun insertRemoteComment(comment: Comment): Boolean {
        val exists = commentDao.checkExists(comment.createdAt, comment.text)
        if (exists == 0) {
            // If local incident has a different SQLite ID, match by createdAt if possible
            val localIncident = if (comment.incidentCreatedAt != 0L) {
                dao.getIncidentByCreatedAt(comment.incidentCreatedAt)
            } else null
            val finalComment = if (localIncident != null && localIncident.id != comment.incidentId) {
                comment.copy(incidentId = localIncident.id)
            } else {
                comment
            }
            commentDao.insertComment(finalComment)
            return true
        }
        return false
    }

    suspend fun deleteIncident(id: Long) {
        commentDao.deleteCommentsForIncident(id)
        dao.deleteById(id)
    }

    suspend fun resetToDefaults() {
        dao.clearAll()
        dao.insertAll(SeedData.INITIAL_INCIDENTS)
    }

    suspend fun seedIfEmpty() {
        if (dao.getIncidentCount() == 0) {
            dao.insertAll(SeedData.INITIAL_INCIDENTS)
        }
    }
}
