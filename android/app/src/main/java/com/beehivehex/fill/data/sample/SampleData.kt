package com.beehivehex.fill.data.sample

import com.beehivehex.fill.domain.model.Cosmetic
import com.beehivehex.fill.domain.model.Hex
import com.beehivehex.fill.domain.model.HiveMode
import com.beehivehex.fill.domain.model.Shape

object SampleData {
    private fun cells(vararg values: Pair<Int, Int>) = values.map { Hex(it.first, it.second) }
    val modes = listOf(HiveMode.CLASSIC, HiveMode.POCKET)
    val shapes = listOf(
        Shape("drop", cells(0 to 0), 12),
        Shape("pair", cells(0 to 0, 1 to 0), 18),
        Shape("trio_line", cells(0 to 0, 1 to 0, 2 to 0), 10),
        Shape("trio_cluster", cells(0 to 0, 1 to 0, 0 to 1), 10),
        Shape("trio_bend", cells(0 to 0, 1 to 0, 1 to 1), 10),
        Shape("four_line", cells(0 to 0, 1 to 0, 2 to 0, 3 to 0), 6),
        Shape("four_hook", cells(0 to 0, 1 to 0, 2 to 0, 2 to -1), 8),
        Shape("four_diamond", cells(0 to 0, 1 to 0, 0 to 1, 1 to 1), 8),
        Shape("four_branch", cells(0 to 0, 1 to 0, -1 to 1, 0 to -1), 6),
        Shape("five_line", cells(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0), 3),
        Shape("five_hook", cells(0 to 0, 1 to 0, 2 to 0, 3 to 0, 3 to -1), 3),
        Shape("five_fan", cells(0 to 0, 1 to 0, 1 to -1, 0 to -1, -1 to 0), 3),
        Shape("five_zigzag", cells(0 to 0, 1 to 0, 1 to 1, 2 to 1, 2 to 2), 3)
    )
    val bees = listOf(Cosmetic("sunny", 0), Cosmetic("clover", 250), Cosmetic("bluebell", 1000), Cosmetic("amber", 2500))
    val frames = listOf(Cosmetic("meadow", 0), Cosmetic("golden", 350), Cosmetic("bluebell", 900), Cosmetic("ivory", 1800))
    val framePreview = cells(0 to 0, 1 to 0, 0 to 1, -1 to 1, -1 to 0, 0 to -1).toSet()
}
