package com.sxtanna.aspiriamc.market.sort

import com.sxtanna.aspiriamc.market.Product

sealed class ProductSorter : Comparator<Product> {

    object ByCost : ProductSorter() {

        override fun compare(o1: Product, o2: Product): Int {
            val value = o1.cost.compareTo(o2.cost)
            return if (value == 0) {
                1
            }
            else {
                value
            }
        }

    }

    object ByName : ProductSorter() {

        override fun compare(o1: Product, o2: Product): Int {
            return o1.name.compareTo(o2.name)
        }

    }

    object ByDate : ProductSorter() {

        override fun compare(o1: Product, o2: Product): Int {
            return o1.date.compareTo(o2.date)
        }

    }

    object ByStaffer : ProductSorter() {

        override fun compare(o1: Product, o2: Product): Int {
            return when {
                o1.stafferUUID == o2.stafferUUID -> {
                    1
                }
                else -> {
                    2
                }
            }
        }

    }

}