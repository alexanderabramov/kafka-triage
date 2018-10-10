package org.kafkatriage.topics

data class Topic(
        val name: String,
        val lag: Long = 0
)