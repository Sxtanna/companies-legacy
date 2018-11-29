package com.sxtanna.aspirianmc.company

import com.sxtanna.aspirianmc.base.Identified
import java.util.*

class Staffer() : Identified<UUID> {
    constructor(uuid: UUID): this() {
        this.uuid = uuid
    }

    override var uuid = UUID.randomUUID()
        internal set


    var companyUUID: UUID? = null
    val voidedItems = mutableListOf<String>()


    internal fun updateData(uuid: UUID, companyUUID: UUID?, voidedItems: List<String>): Staffer {
        this.uuid =  uuid
        this.companyUUID = companyUUID
        this.voidedItems += voidedItems

        return this
    }


    enum class AccountType {

        NEW,
        OLD,

    }

}