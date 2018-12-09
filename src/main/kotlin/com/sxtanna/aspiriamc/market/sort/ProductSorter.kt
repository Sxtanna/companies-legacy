package com.sxtanna.aspiriamc.market.sort

import com.sxtanna.aspiriamc.market.Product

sealed class ProductSorter : Comparator<Product> {

    object ByCost : ProductSorter() {

        override fun compare(o1: Product, o2: Product): Int {
            return o1.cost.compareTo(o2.cost)
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
            val uuid0 = o1.stafferUUID
            val uuid1 = o2.stafferUUID

            return when {
                uuid0 == null -> {
                    -1
                }
                uuid1 == null -> {
                    +1
                }
                else -> {
                    uuid0.compareTo(uuid1)
                }
            }
        }

    }

}