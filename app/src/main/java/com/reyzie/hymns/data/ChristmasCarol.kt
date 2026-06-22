package com.reyzie.hymns.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChristmasCarol(
    val id: String,
    val title: String,
    @SerialName("song_number")
    val songNumber: String? = null,
    @SerialName("church_name")
    val churchName: String,
    val lyrics: String? = null,
    @SerialName("pdf")
    val pdfPath: String? = null,
    @SerialName("pdf_pages")
    val pdfPages: List<String>? = null,
    val transpose: Int = 0,
    val scale: String = "C Major",
    @SerialName("has_chords")
    val hasChords: Boolean = true,
    @SerialName("created_by_user_id")
    val createdByUserId: String = "",
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val hasPdf: Boolean
        get() = !pdfPath.isNullOrEmpty()

    val hasLyrics: Boolean
        get() = !lyrics.isNullOrEmpty()
}

object MusicalScales {
    val majorScales = listOf(
        "C Major", "C# Major", "D Major", "D# Major", "E Major", "F Major",
        "F# Major", "G Major", "G# Major", "A Major", "A# Major", "B Major"
    )

    val minorScales = listOf(
        "C Minor", "C# Minor", "D Minor", "D# Minor", "E Minor", "F Minor",
        "F# Minor", "G Minor", "G# Minor", "A Minor", "A# Minor", "B Minor"
    )

    val allScales = majorScales + minorScales

    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun transposeScale(scale: String, semitones: Int): String {
        val isMajor = scale.contains("Major")
        val suffix = if (isMajor) " Major" else " Minor"
        val notePart = scale.replace(" Major", "").replace(" Minor", "")

        var noteIndex = notes.indexOf(notePart)
        if (noteIndex == -1) return scale

        noteIndex = (noteIndex + semitones) % 12
        if (noteIndex < 0) noteIndex += 12

        return notes[noteIndex] + suffix
    }
}
