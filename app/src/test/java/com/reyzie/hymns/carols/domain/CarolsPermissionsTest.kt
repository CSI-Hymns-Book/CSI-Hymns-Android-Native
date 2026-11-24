package com.reyzie.hymns.carols.domain

import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolSong
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarolsPermissionsTest {
    private val church = CarolChurch(id = "c1", name = "Bethel", createdByUserId = "church-owner", createdAt = "t")
    private val song = CarolSong(
        id = "s1", churchId = "c1", title = "Joy", lyrics = "x", createdByUserId = "song-uploader", createdAt = "t",
    )
    private val admin = "reyziecrafts@gmail.com"

    @Test
    fun `the church's uploader can delete its church songs and pdfs`() {
        assertTrue(CarolsPermissions.canDeleteChurch(church, "church-owner", "owner@example.com"))
        assertTrue(CarolsPermissions.canDeleteSong(church, "church-owner", "owner@example.com"))
        assertTrue(CarolsPermissions.canDeletePdf(church, "church-owner", "owner@example.com"))
    }

    @Test
    fun `an admin email can delete anything`() {
        assertTrue(CarolsPermissions.canDeleteChurch(church, "someone", admin))
        assertTrue(CarolsPermissions.canDeleteSong(church, "someone", admin))
        assertTrue(CarolsPermissions.canDeletePdf(null, "someone", admin))
    }

    @Test
    fun `the admin check ignores case and spaces`() {
        assertTrue(CarolsPermissions.isAdmin("  ReyzieCrafts@Gmail.com "))
    }

    @Test
    fun `a song's own uploader cannot delete it unless they uploaded the church`() {
        assertFalse(CarolsPermissions.canDeleteSong(church, "song-uploader", "uploader@example.com"))
    }

    @Test
    fun `other signed-in users cannot delete`() {
        assertFalse(CarolsPermissions.canDeleteChurch(church, "stranger", "stranger@example.com"))
        assertFalse(CarolsPermissions.canDeleteSong(church, "stranger", "stranger@example.com"))
        assertFalse(CarolsPermissions.canDeletePdf(church, "stranger", "stranger@example.com"))
    }

    @Test
    fun `signed-out users cannot delete or edit`() {
        assertFalse(CarolsPermissions.canDeleteChurch(church, null, null))
        assertFalse(CarolsPermissions.canDeleteSong(church, null, null))
        assertFalse(CarolsPermissions.canEditSong(song, church, null, null))
    }

    @Test
    fun `an unknown church can only be managed by an admin`() {
        assertFalse(CarolsPermissions.canDeleteSong(null, "church-owner", "owner@example.com"))
        assertTrue(CarolsPermissions.canDeleteSong(null, "x", admin))
    }

    @Test
    fun `editing is open to the church uploader the song uploader and admins`() {
        assertTrue(CarolsPermissions.canEditSong(song, church, "church-owner", null))
        assertTrue(CarolsPermissions.canEditSong(song, church, "song-uploader", null))
        assertTrue(CarolsPermissions.canEditSong(song, church, "anyone", admin))
        assertFalse(CarolsPermissions.canEditSong(song, church, "stranger", "stranger@example.com"))
    }

    @Test
    fun `a missing email never counts as an admin`() {
        assertFalse(CarolsPermissions.isAdmin(null))
        assertFalse(CarolsPermissions.isAdmin(""))
    }
}
