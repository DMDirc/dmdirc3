package com.dmdirc.ui.nicklist

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

val nickListModule = Kodein.Module(name = "NickList") {
    bind<NickListModel>() with singleton { NickListModel(instance()) }
    bind<NickListBinder>() with singleton { NickListBinder() }
    bind<NickListView>() with provider {
        NickListView().also {
            instance<NickListBinder>().bind(instance(), it)
        }
    }
}
