package com.sxtanna.aspiriamc.company

import com.sxtanna.aspiriamc.base.Unique
import java.util.*

class Staffer() : Unique<UUID> {
    constructor(uuid: UUID) : this() {
        this.uuid = uuid
    }

    override var uuid = UUID.randomUUID()
        internal set


    var companyUUID: UUID? = null
    val voidedItems = mutableListOf<String>()


    internal fun updateData(uuid: UUID, companyUUID: UUID?, voidedItems: List<String>): Staffer {
        this.uuid = uuid
        this.companyUUID = companyUUID
        this.voidedItems += voidedItems

        return this
    }


    override fun toString(): String {
        return "Staffer(uuid=$uuid, companyUUID=$companyUUID, voidedItems=$voidedItems)"
    }


    enum class AccountType {

        NEW,
        OLD,

    }

}