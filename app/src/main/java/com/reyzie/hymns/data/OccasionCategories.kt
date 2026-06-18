package com.reyzie.hymns.data

/**
 * Curated hymn & keerthane lists per worship occasion (mirrors Flutter `lib/screens/categories.dart`).
 */
data class OccasionCategory(
    val name: String,
    val hymnNumbers: List<Int>,
    val keerthaneNumbers: List<Int>,
) {
    val hasHymns: Boolean get() = hymnNumbers.isNotEmpty()
    val hasKeerthanes: Boolean get() = keerthaneNumbers.isNotEmpty()
    val isEmpty: Boolean get() = !hasHymns && !hasKeerthanes
}

object OccasionCategories {
    private val categories = listOf(
        OccasionCategory("Birthday", hymnNumbers = listOf(361), keerthaneNumbers = listOf(215)),
        OccasionCategory(
            "Marriage",
            hymnNumbers = listOf(358, 359, 360),
            keerthaneNumbers = listOf(188, 189, 190),
        ),
        OccasionCategory(
            "House Warming",
            hymnNumbers = listOf(362),
            keerthaneNumbers = listOf(227, 228, 229, 230, 231, 232, 233, 234),
        ),
        OccasionCategory("Funeral", hymnNumbers = emptyList(), keerthaneNumbers = emptyList()),
        OccasionCategory(
            "Mangala",
            hymnNumbers = emptyList(),
            keerthaneNumbers = listOf(227, 228, 229, 230, 231, 232, 233, 234),
        ),
        OccasionCategory(
            "Children's Prayer",
            hymnNumbers = (328..349).toList(),
            keerthaneNumbers = (200..209).toList(),
        ),
        OccasionCategory(
            "Lord's Supper",
            hymnNumbers = (273..279).toList(),
            keerthaneNumbers = listOf(184, 185, 186, 187),
        ),
        OccasionCategory("Travelling", hymnNumbers = listOf(363), keerthaneNumbers = emptyList()),
        OccasionCategory("Sickness", hymnNumbers = listOf(367), keerthaneNumbers = emptyList()),
    )

    val allNames: List<String> = categories.map { it.name }

    fun find(name: String): OccasionCategory? = categories.find { it.name == name }
}
