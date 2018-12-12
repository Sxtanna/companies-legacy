package com.sxtanna.aspiriamc.menu.base

enum class Col {

    C_1,
    C_2,
    C_3,
    C_4,
    C_5,
    C_6,
    C_7,
    C_8,
    C_9;


    fun slot(row: Row): Int {
        return (row.ordinal * 9) + ordinal
    }

}