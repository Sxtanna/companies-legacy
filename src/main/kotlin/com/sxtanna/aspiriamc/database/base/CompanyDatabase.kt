package com.sxtanna.aspiriamc.database.base

import com.sxtanna.aspiriamc.base.Named
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.reports.Format
import com.sxtanna.aspiriamc.reports.Reports
import java.util.UUID

interface CompanyDatabase : Named, PluginDependant {

    fun load()

    fun kill()


    fun saveCompany(data: Company) {
        saveCompany(data, true)
    }

    fun saveCompany(data: Collection<Company>)

    fun saveCompany(data: Company, async: Boolean)

    fun loadCompany(uuid: UUID, returnSync: Boolean = true, onLoad: (company: Company?) -> Unit)

    fun killCompany(uuid: UUID)


    fun allCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)

    fun topCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)


    fun saveStaffer(data: Staffer)

    fun loadStaffer(uuid: UUID, returnSync: Boolean = true, onLoad: (staffer: Staffer?) -> Unit)


    fun saveReports(data: Reports)

    fun loadReports(format: Format, before: Long, returnSync: Boolean = true, onLoad: (List<Reports>) -> Unit)

}