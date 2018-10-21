package org.kafkatriage.records

import org.kafkatriage.topics.Topic
import org.kafkatriage.topics.TopicPartition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface RecordRepository : JpaRepository<Record, UUID> {
    @Query("select new org.kafkatriage.topics.Topic(r.topic, count(*)) from Record r group by r.topic")
    fun listTopics(): List<Topic>

    @Query("select new org.kafkatriage.topics.TopicPartition(r.topic, r.partition, count(*)) from Record r group by r.topic, r.partition")
    fun listPartitions(): List<TopicPartition>

    fun findByTopicAndTriaged(topic: String, triaged: Boolean = false): List<Record>

    @Query("select r from Record r where topic=:topic and partition=:partition and offset<=:offset")
    fun findUnTriagedTo(topic: String, partition: Int, offset: Long): List<Record>

    @Modifying
    @Query("update Record r set triaged=true where topic=:topic and partition=:partition and offset<=:offset")
    @Transactional
    fun markTriagedTo(topic: String, partition: Int, offset: Long)

    @Modifying
    @Query("update Record r set replayedOffset=:replayedOffset where id=:id")
    @Transactional
    fun setReplayedOffset(id: Int, replayedOffset: Long)

}
