package com.sxtanna.aspiriamc.reports

import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Unique
import com.sxtanna.aspiriamc.reports.Format.*
import com.sxtanna.aspiriamc.reports.Report.Codec
import com.sxtanna.aspiriamc.reports.Report.Purchase.*
import com.sxtanna.korm.Korm
import com.sxtanna.korm.base.KormPuller
import com.sxtanna.korm.data.KormType
import com.sxtanna.korm.data.custom.KormCustomPull
import com.sxtanna.korm.reader.KormReader.ReaderContext
import org.bukkit.Material
import java.time.Instant
import java.util.*

@KormCustomPull(Codec::class)
sealed class Report : Unique<UUID> {

    final override var uuid = UUID.randomUUID()
        private set


    val occurredAt = Instant.now().toEpochMilli()

    abstract val format: Format
    abstract val amount: Double


    object None : Report() {

        override val format = NONE
        override val amount = -1.0

    }

    data class SellItem(override val amount: Double, val by: UUID, val idItem: UUID, val idComp: UUID) : Report() {

        override val format = SELL_ITEM

    }

    data class TakeItem(override val amount: Double, val to: UUID, val idItem: UUID, val idComp: UUID) : Report() {

        override val format = TAKE_ITEM

    }

    sealed class Purchase : Report() {

        data class Comp(override val amount: Double, val by: UUID, val idComp: UUID, val named: String) : Purchase() {

            override val format = PURCHASE_COMP

        }

        data class Icon(override val amount: Double, val by: UUID, val idComp: UUID, val oldType: Material, val newType: Material) : Purchase() {

            override val format = PURCHASE_ICON

        }

        data class Item(override val amount: Double, val from: UUID, val into: UUID, val base: String, val idItem: UUID, val idComp: UUID, val transactions: Map<UUID, Double>) : Purchase() {

            override val format = PURCHASE_ITEM

        }

    }


    object Codec : KormPuller<Report> {

        override fun pull(reader: ReaderContext, types: MutableList<KormType>): Report? {
            return when (reader.mapData<Format>(types.byName("format")) ?: return null) {
                NONE          -> {
                    return None
                }
                SELL_ITEM     -> {
                    reader.mapInstance(SellItem::class, types)
                }
                TAKE_ITEM     -> {
                    reader.mapInstance(TakeItem::class, types)
                }
                PURCHASE_COMP -> {
                    reader.mapInstance(Comp::class, types)
                }
                PURCHASE_ICON -> {
                    reader.mapInstance(Icon::class, types)
                }
                PURCHASE_ITEM -> {
                    reader.mapInstance(Item::class, types)
                }
            }
        }

    }

    object Maker {

        fun make(korm: Korm, text: String): Report {
            val reportResult = Result.of {
                korm.pull(text).to<Report>()
            }

            return when (reportResult) {
                is Result.Some -> {
                    reportResult.data
                }
                is Result.None -> {
                    return Report.None
                }
            }
        }

    }

}