package com.beehivehex.fill.core.ui

import com.beehivehex.fill.R
import com.beehivehex.fill.domain.model.HiveMode

fun modeLabel(mode: HiveMode) = if (mode == HiveMode.CLASSIC) R.string.classic else R.string.pocket
fun beeLabel(id: String) = when (id) {
    "clover" -> R.string.bee_clover
    "bluebell" -> R.string.bee_bluebell
    "amber" -> R.string.bee_amber
    else -> R.string.bee_sunny
}
fun beeDescription(id: String) = when (id) {
    "clover" -> R.string.clover_description
    "bluebell" -> R.string.bluebell_description
    "amber" -> R.string.amber_description
    else -> R.string.sunny_description
}
fun frameLabel(id: String) = when (id) {
    "golden" -> R.string.frame_golden
    "bluebell" -> R.string.frame_bluebell
    "ivory" -> R.string.frame_ivory
    else -> R.string.frame_meadow
}
fun rewardLabel(id: String) = if (id.startsWith("bee:")) beeLabel(id.substringAfter(':')) else frameLabel(id.substringAfter(':'))
fun shapeLabel(id: String) = when (id) {
    "pair" -> R.string.shape_pair
    "trio_line" -> R.string.shape_trio_line
    "trio_cluster" -> R.string.shape_trio_cluster
    "trio_bend" -> R.string.shape_trio_bend
    "four_line" -> R.string.shape_four_line
    "four_hook" -> R.string.shape_four_hook
    "four_diamond" -> R.string.shape_four_diamond
    "four_branch" -> R.string.shape_four_branch
    "five_line" -> R.string.shape_five_line
    "five_hook" -> R.string.shape_five_hook
    "five_fan" -> R.string.shape_five_fan
    "five_zigzag" -> R.string.shape_five_zigzag
    else -> R.string.shape_drop
}
