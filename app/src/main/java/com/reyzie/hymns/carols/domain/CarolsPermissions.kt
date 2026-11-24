package com.reyzie.hymns.carols.domain

import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong

/**
 * Who may change community carols.
 *
 * Deleting a church, or a song or PDF inside it, is only for the person who uploaded the church
 * and for admins. Editing a song is also open to whoever uploaded that song. Everyone else, and
 * anyone signed out, can only read.
 */
object CarolsPermissions {
    private val ADMIN_EMAILS = setOf(
        "reynoldclare29022902@gmail.com",
        "reynoldclare02@gmail.com",
        "reyziecrafts@gmail.com",
        "reynold.clare29022902@gmail.com",
    )

    fun isAdmin(userEmail: String?): Boolean =
        userEmail?.trim()?.lowercase() in ADMIN_EMAILS

    private fun uploadedChurch(church: CarolChurch?, userId: String?): Boolean =
        userId != null && church != null && church.createdByUserId == userId

    fun canDeleteChurch(church: CarolChurch, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || uploadedChurch(church, userId)

    /** [church] is the song's church; a song whose church is unknown can only be deleted by an admin. */
    fun canDeleteSong(church: CarolChurch?, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || uploadedChurch(church, userId)

    fun canDeletePdf(church: CarolChurch?, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || uploadedChurch(church, userId)

    /** The church's uploader, the song's own uploader, or an admin. */
    fun canEditSong(song: CarolSong, church: CarolChurch?, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || uploadedChurch(church, userId) || (userId != null && song.createdByUserId == userId)
}
