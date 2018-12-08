package com.sxtanna.aspiriamc.store.base

import com.sxtanna.aspiriamc.base.Identified

interface Store<T : Identified<I>, I : Any> {

    fun save(data: T)

    fun load(uuid: I): T?

}