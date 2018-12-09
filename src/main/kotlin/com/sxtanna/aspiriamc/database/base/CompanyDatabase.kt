package com.sxtanna.aspiriamc.database.base

import com.sxtanna.aspiriamc.base.Named
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import java.util.*

interface CompanyDatabase : Named, PluginDependant {

    fun load()

    fun kill()


    fun loadCompany(uuid: UUID, returnSync: Boolean = true, onLoad: (company: Company?) -> Unit)

    fun killCompany(uuid: UUID)

    fun saveCompany(data: Company) {
        saveCompany(data, true)
    }

    fun saveCompany(data: Company, async: Boolean)


    fun allCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)

    fun topCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)


    fun loadStaffer(uuid: UUID, returnSync: Boolean = true, onLoad: (staffer: Staffer?) -> Unit)

    fun saveStaffer(data: Staffer)

}