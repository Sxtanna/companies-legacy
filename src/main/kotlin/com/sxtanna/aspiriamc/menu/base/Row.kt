package com.sxtanna.aspiriamc.menu.base

enum class Row {

    R_1,
    R_2,
    R_3,
    R_4,
    R_5,
    R_6;

    val size = (ordinal + 1) * 9


    fun slot(col: Col): Int {
        return (ordinal * 9) + col.ordinal
    }

    fun next(): Row {
        return values().getOrElse(ordinal + 1) { this }
    }


    enum class FillMode {

        ALL,
        CEN,

    }


    companion object {

        fun appropriate(slots: Int, mode: FillMode = FillMode.ALL): Row {
            return when(mode) {
                FillMode.ALL -> {
                    values().first { it.size >= slots }
                }
                FillMode.CEN -> {
                    var slot = 10
                    var left = slots

                    while (left > 0) {
                        slot++
                        left--

                        if (slot == 17 || slot == 26 || slot == 35) {
                            slot += 2
                        }

                        if (slot == 44) {
                            throw IllegalStateException("You can't use center fill mode for $slots slots")
                        }
                    }

                    val result = values().first { it.size >= slot }

                    if (result.ordinal == values().lastIndex) result
                    else {
                        values()[result.ordinal + 1]
                    }

                }
            }
        }

    }

}