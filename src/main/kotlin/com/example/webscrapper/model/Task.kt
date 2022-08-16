package com.example.webscrapper.model

import java.util.*
import javax.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["url", "xpath"])])
open class Task(

    @Column(nullable = false, columnDefinition = "varchar(1024)")
    open var url: String,

    @Column(nullable = false, columnDefinition = "varchar(1024)")
    open var xpath: String,

    @Column(nullable = false)
    open var enabled: Boolean = false,

    @Column(nullable = false)
    open var intervalMillis: Long = 60 * 60 * 1000,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Task
        return url == other.url && xpath == other.xpath
    }

    override fun hashCode(): Int = Objects.hash(url, xpath)

}