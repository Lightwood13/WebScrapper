package com.example.webscrapper.model

import org.hibernate.annotations.CreationTimestamp
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
open class ParseResult(

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = true)
    open var task: Task?,

    @Lob
    @Column(nullable = false)
    open var result: String
) {
    @CreationTimestamp
    @Column(nullable = false)
    open var parsedOn: Timestamp? = null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParseResult
        return task == other.task && parsedOn == other.parsedOn
    }

    override fun hashCode(): Int = Objects.hash(task, parsedOn)

}