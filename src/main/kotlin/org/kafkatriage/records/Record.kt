package org.kafkatriage.records

data class Record(
        val partition: Int,
        val offset: Long,
        val key: String?,
        val value: String?,
        val headers: List<Pair<String, String>>?
)