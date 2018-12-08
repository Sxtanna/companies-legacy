package com.sxtanna.aspiriamc.database.base

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.database.KueryDatabase
import com.sxtanna.aspiriamc.database.LocalDatabase

enum class DatabaseType(private val create: (Companies) -> CompanyDatabase) {

    LOCAL(::LocalDatabase),
    KUERY(::KueryDatabase);


    fun createDatabase(plugin: Companies): CompanyDatabase {
        return create.invoke(plugin)
    }


    companion object {

        fun get(name: String) = try {
            valueOf(name.toUpperCase())
        }
        catch (ignored: Exception) {
            LOCAL
        }

    }

}