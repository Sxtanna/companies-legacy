package com.sxtanna.aspiriamc.database

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.database.base.CompanyDatabase
import java.util.*

class RedisDatabase(override val plugin: Companies) : CompanyDatabase {

    override val name = "Redis"


    override fun load() {
        TODO("not implemented")
    }

    override fun kill() {
        TODO("not implemented")
    }

    override fun loadCompany(uuid: UUID, returnSync: Boolean, onLoad: (company: Company?) -> Unit) {
        TODO("not implemented")
    }

    override fun killCompany(uuid: UUID) {
        TODO("not implemented")
    }

    override fun saveCompany(data: Company, async: Boolean) {
        TODO("not implemented")
    }

    override fun allCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) {
        TODO("not implemented")
    }

    override fun topCompanies(returnSync: Boolean, onRetrieve: (companies: List<Company>) -> Unit) {
        TODO("not implemented")
    }

    override fun loadStaffer(uuid: UUID, returnSync: Boolean, onLoad: (staffer: Staffer?) -> Unit) {
        TODO("not implemented")
    }

    override fun saveStaffer(data: Staffer) {
        TODO("not implemented")
    }
}