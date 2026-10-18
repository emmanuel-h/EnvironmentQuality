package fr.mandarine.environmentquality.presentation

/** What a screen section shows while its data is fetched. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
    data class Failed(val cause: Throwable) : LoadState<Nothing>

    val valueOrNull: T? get() = (this as? Loaded<T>)?.value
}

/** Runs [block] and wraps its outcome, letting cancellation through. */
suspend fun <T> load(block: suspend () -> T): LoadState<T> = try {
    LoadState.Loaded(block())
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    LoadState.Failed(e)
}
