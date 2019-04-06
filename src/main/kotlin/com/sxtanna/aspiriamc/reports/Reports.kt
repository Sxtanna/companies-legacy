package com.sxtanna.aspiriamc.reports

import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Unique
import com.sxtanna.aspiriamc.reports.Format.*
import com.sxtanna.aspiriamc.reports.Reports.Codec
import com.sxtanna.aspiriamc.reports.Reports.Staffing.*
import com.sxtanna.aspiriamc.reports.Reports.Transaction.Purchase.*
import com.sxtanna.aspiriamc.reports.Reports.Transaction.SellItem
import com.sxtanna.aspiriamc.reports.Reports.Transaction.TakeItem
import com.sxtanna.korm.Korm
import com.sxtanna.korm.base.KormPuller
import com.sxtanna.korm.data.KormType
import com.sxtanna.korm.data.custom.KormCustomPull
import com.sxtanna.korm.reader.KormReader.ReaderContext
import org.bukkit.Material
import java.time.Instant
import java.util.UUID

@KormCustomPull(Codec::class)
sealed class Reports : Unique<UUID> {

    final override var uuid = UUID.randomUUID()
        private set


    val occurredAt = Instant.now().toEpochMilli()


    abstract val format: Format


    object None : Reports() {

        override val format = NONE

    }


    data class Deletion(val idComp: UUID, val by: UUID) : Reports() {

        override val format = COMPANY_DELETION

    }

    data class Withdraw(val idComp: UUID, val to: UUID, val amount: Double) : Reports() {

        override val format = COMPANY_WITHDRAW

    }

    data class Transfer(val idComp: UUID, val by: UUID, val to: UUID) : Reports() {

        override val format = COMPANY_TRANSFER

    }


    sealed class Transaction : Reports() {

        abstract val amount: Double
        abstract val idComp: UUID


        data class SellItem(override val amount: Double, override val idComp: UUID, val by: UUID, val idItem: UUID, val base: String? = null) : Transaction() {

            override val format = SELL_ITEM

        }

        data class TakeItem(override val amount: Double, override val idComp: UUID, val to: UUID, val idItem: UUID, val base: String? = null) : Transaction() {

            override val format = TAKE_ITEM

        }

        sealed class Purchase : Transaction() {

            data class Comp(override val amount: Double, override val idComp: UUID, val by: UUID, val named: String) : Purchase() {

                override val format = PURCHASE_COMP

            }

            data class Icon(override val amount: Double, override val idComp: UUID, val by: UUID, val oldType: Material, val newType: Material) : Purchase() {

                override val format = PURCHASE_ICON

            }

            data class Item(override val amount: Double, override val idComp: UUID, val from: UUID, val into: UUID, val base: String, val idItem: UUID, val transactions: Map<UUID, Double>) : Purchase() {

                override val format = PURCHASE_ITEM

            }

            data class Name(override val amount: Double, override val idComp: UUID, val by: UUID, val oldName: String, val newName: String) : Purchase() {

                override val format = PURCHASE_NAME

            }

        }

    }

    sealed class Staffing : Reports() {

        abstract val idComp: UUID
        abstract val idUser: UUID


        data class Hire(override val idComp: UUID, override val idUser: UUID) : Staffing() {

            override val format = COMPANY_STAFF_HIRE

        }

        data class Fire(override val idComp: UUID, override val idUser: UUID, val idFiredBy: UUID) : Staffing() {

            override val format = COMPANY_STAFF_FIRE

        }

        data class Quit(override val idComp: UUID, override val idUser: UUID) : Staffing() {

            override val format = COMPANY_STAFF_QUIT

        }

    }


    object Codec : KormPuller<Reports> {

        override fun pull(reader: ReaderContext, types: MutableList<KormType>): Reports? {
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
                PURCHASE_NAME -> {
                    reader.mapInstance(Name::class, types)
                }
                COMPANY_DELETION -> {
                    reader.mapInstance(Deletion::class, types)
                }
                COMPANY_WITHDRAW -> {
                    reader.mapInstance(Withdraw::class, types)
                }
                COMPANY_TRANSFER -> {
                    reader.mapInstance(Transfer::class, types)
                }
                COMPANY_STAFF_HIRE -> {
                    reader.mapInstance(Hire::class, types)
                }
                COMPANY_STAFF_FIRE -> {
                    reader.mapInstance(Fire::class, types)
                }
                COMPANY_STAFF_QUIT -> {
                    reader.mapInstance(Quit::class, types)
                }
            }
        }

    }

    object Maker {

        fun make(korm: Korm, text: String): Reports {
            val reportResult = Result.of {
                korm.pull(text).to<Reports>()
            }

            return when (reportResult) {
                is Result.Some -> {
                    reportResult.data
                }
                is Result.None -> {
                    return Reports.None
                }
            }
        }

    }

}