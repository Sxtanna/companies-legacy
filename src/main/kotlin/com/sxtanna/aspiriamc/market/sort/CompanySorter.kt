package com.sxtanna.aspiriamc.market.sort

import com.sxtanna.aspiriamc.company.Company

sealed class CompanySorter : Comparator<Company> {

    object Natural : CompanySorter() {

        override fun compare(o1: Company, o2: Company): Int {
            return 1
        }

    }

    object ByName : CompanySorter() {

        override fun compare(o1: Company, o2: Company): Int {
            return o1.name.compareTo(o2.name)
        }

    }

    object ByProductCount : CompanySorter() {

        override fun compare(o1: Company, o2: Company): Int {
            return o1.product.size.compareTo(o2.product.size)
        }

    }

    object ByStafferCount : CompanySorter() {

        override fun compare(o1: Company, o2: Company): Int {
            return o1.staffer.size.compareTo(o2.staffer.size)
        }

    }

    object ByPopularity : CompanySorter() {

        override fun compare(o1: Company, o2: Company): Int {
            val pop = o1.plugin.reportsManager.popularity

            val o1Pop = pop[o1.uuid]
            val o2Pop = pop[o2.uuid]

            return o1Pop.compareTo(o2Pop)
        }

    }

}