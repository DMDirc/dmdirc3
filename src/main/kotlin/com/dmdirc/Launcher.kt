package com.dmdirc

import com.install4j.api.launcher.Variables

fun checkLauncher() = try {
    Class.forName("com.install4j.api.launcher.Variables") != null
} catch (e: ReflectiveOperationException) {
    false
}

fun getVersion(): String = if (!checkLauncher()) {
    "dev"
} else {
    try {
        Variables.getCompilerVariable("sys.version") ?: "dev"
    } catch (e: ReflectiveOperationException) {
        "dev"
    }
}