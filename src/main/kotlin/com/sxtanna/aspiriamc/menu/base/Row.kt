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

}