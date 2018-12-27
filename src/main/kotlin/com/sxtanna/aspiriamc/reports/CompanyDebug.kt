package com.sxtanna.aspiriamc.reports

import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer

data class CompanyDebug(val requestedBy: String, val companyInfo: CompanyInfo, val stafferInfo: StafferInfo, val aspiriaInfo: AspiriaInfo) {

    data class CompanyInfo(val company: List<Company>)

    data class StafferInfo(val staffer: List<Staffer>)

    data class AspiriaInfo(val memory: MemoryState, val server: ServerState, val plugin: PluginState) {

        data class MemoryState(val used: Long, val free: Long)

        data class ServerState(val tick: List<Double>, val paper: PaperInfo) {

            data class PaperInfo(val version: String, val bukkitVersion: String)

        }

        data class PluginState(val plugins: List<PluginInfo>) {

            data class PluginInfo(val name: String, val version: String, val apiVersion: String, val author: String)

        }

    }

}