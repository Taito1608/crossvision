package com.example.solution_development

object CapturedImageStore {
    private val images = mutableListOf<ByteArray>()
    private val listeners = mutableListOf<(ByteArray) -> Unit>()

    @Synchronized
    fun addImage(bytes: ByteArray) {
        images.add(0, bytes)
        for (l in listeners) {
            try { l(bytes) } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun getImages(): List<ByteArray> = images.toList()

    @Synchronized
    fun clear() { images.clear() }

    @Synchronized
    fun addListener(l: (ByteArray) -> Unit) { listeners.add(l) }

    @Synchronized
    fun removeListener(l: (ByteArray) -> Unit) { listeners.remove(l) }
}
