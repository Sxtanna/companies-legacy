package com.sxtanna.aspirianmc.database.base

import com.sxtanna.aspirianmc.base.Named
import com.sxtanna.aspirianmc.base.PluginDependant
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import java.util.*

interface CompanyDatabase : Named, PluginDependant {

    fun load()

    fun kill()


    fun loadCompany(uuid: UUID, returnSync: Boolean = true, onLoad: (company: Company?) -> Unit)

    fun killCompany(uuid: UUID)

    fun saveCompany(data: Company)


    fun allCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)

    fun topCompanies(returnSync: Boolean = true, onRetrieve: (companies: List<Company>) -> Unit)


    fun loadStaffer(uuid: UUID, returnSync: Boolean = true, onLoad: (staffer: Staffer?) -> Unit)

    fun saveStaffer(data: Staffer)

}