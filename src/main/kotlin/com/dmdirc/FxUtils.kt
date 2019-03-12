package com.dmdirc

import com.bugsnag.Bugsnag
import com.dmdirc.PlatformWrappers.fxThreadTester
import com.dmdirc.PlatformWrappers.runLaterProvider
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import org.kodein.di.direct
import org.kodein.di.generic.instance

fun <T> List<T>.observable(): ObservableList<T> = FXCollections.observableList(this)
fun <T> Set<T>.observable(): ObservableSet<T> = FXCollections.observableSet(this)
fun <T> ObservableSet<T>.synchronized(): ObservableSet<T> = FXCollections.synchronizedObservableSet(this)
fun <T> ObservableSet<T>.readOnly(): ObservableSet<T> = FXCollections.unmodifiableObservableSet(this)
fun <K, V> Map<K, V>.observable(): ObservableMap<K, V> = FXCollections.observableMap(this)

fun <T, Y> Property<T>.bindTransform(other: Property<Y>, biFunction: (T, T) -> Y) {
    addListener { _, oldValue, newValue ->
        other.value = biFunction(oldValue, newValue)
    }
}

fun runLater(block: () -> Unit) = runLaterProvider(Runnable(block))

fun assertOnFxThread(bugsnag: Bugsnag? = null) {
    if (!fxThreadTester()) {
        val stackTrace = Throwable().stackTrace
        val method = stackTrace[1].methodName
        val exception =
            WrongThreadException("Function $method must be called on the FX thread. Current thread: ${Thread.currentThread().name}")
        exception.stackTrace = stackTrace.sliceArray(2 until stackTrace.size)
        (bugsnag ?: kodein.direct.instance()).notify(exception)
    }
}

/**
 * Implements the [Property] interface and proxies all methods to the parent.
 *
 * Useful if you want to override one method and leave all the others alone.
 */
@Suppress("UsePropertyAccessSyntax")
internal abstract class ProxyProperty<T>(private val parent: Property<T>) : Property<T> {
    override fun setValue(value: T) = parent.setValue(value)
    override fun getName(): String = parent.name
    override fun bindBidirectional(other: Property<T>?) = parent.bindBidirectional(other)
    override fun addListener(listener: ChangeListener<in T>?) = parent.addListener(listener)
    override fun addListener(listener: InvalidationListener?) = parent.addListener(listener)
    override fun getBean(): Any = parent.bean
    override fun unbind() = parent.unbind()
    override fun removeListener(listener: ChangeListener<in T>?) = parent.removeListener(listener)
    override fun removeListener(listener: InvalidationListener?) = parent.removeListener(listener)
    override fun bind(observable: ObservableValue<out T>?) = parent.bind(observable)
    override fun isBound(): Boolean = parent.isBound
    override fun getValue(): T = parent.value
    override fun unbindBidirectional(other: Property<T>?) = parent.unbindBidirectional(other)
}

/**
 * Creates a proxy over the property that asserts that sets are always performed on the correct thread.
 */
fun <T> Property<T>.threadAsserting(): Property<T> = object : ProxyProperty<T>(this) {
    override fun setValue(value: T) {
        assertOnFxThread()
        super.setValue(value)
    }
}

fun Property<Boolean>.not() = object : BooleanBinding() {
    init {
        super.bind(this@not)
    }
    override fun dispose() = super.unbind(this@not)
    override fun computeValue() = !this@not.value
    override fun getDependencies() = FXCollections.singletonObservableList<Property<Boolean>>(this@not)
}

fun Property<*>.isNull() = object : BooleanBinding() {
    init {
        super.bind(this@isNull)
    }
    override fun dispose() = super.unbind(this@isNull)
    override fun computeValue() = this@isNull.value == null
    override fun getDependencies() = FXCollections.singletonObservableList<Property<*>>(this@isNull)
}

internal class WrongThreadException(message: String) : RuntimeException(message)

// For testing purposes: we can swap out the Platform calls to something we control
internal object PlatformWrappers {
    internal var runLaterProvider: (Runnable) -> Unit = Platform::runLater
    internal var fxThreadTester: () -> Boolean = Platform::isFxApplicationThread
}
