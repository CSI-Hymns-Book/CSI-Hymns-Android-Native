package com.reyzie.hymns.carols.domain

import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong

object CarolsPermissions {
    private val ADMIN_EMAILS = setOf(
        "reynoldclare29022902@gmail.com",
        "reynoldclare02@gmail.com",
        "reyziecrafts@gmail.com",
        "reynold.clare29022902@gmail.com",
    )

    fun isAdmin(userEmail: String?): Boolean =
        userEmail?.lowercase() in ADMIN_EMAILS

    fun canDeleteChurch(church: CarolChurch, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || church.createdByUserId == userId

    fun canDeleteSong(song: CarolSong, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || song.createdByUserId == userId

    fun canDeletePdf(pdf: CarolPdf, userId: String?, userEmail: String?): Boolean =
        isAdmin(userEmail) || pdf.createdByUserId == userId
}
