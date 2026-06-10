package llc.lookatwhataicando.codeoba.core.domain.source

class SourceRegistry {
    private val adapters = mutableListOf<SourceAdapter>()
    private var ignoredSourceIds = emptySet<String>()

    fun register(adapter: SourceAdapter) {
        adapters.add(adapter)
    }

    fun setIgnoredSources(ids: Set<String>) {
        ignoredSourceIds = ids
    }

    fun getAllAdapters(): List<SourceAdapter> =
        adapters.sortedWith(
            compareByDescending<SourceAdapter> { it.isAppInstalled() }
                .thenBy { it.displayName }
        )

    fun getActiveAdapters(): List<SourceAdapter> =
        adapters.filter { it.isAvailable() && it.id !in ignoredSourceIds }
            .sortedBy { it.displayName }
}
