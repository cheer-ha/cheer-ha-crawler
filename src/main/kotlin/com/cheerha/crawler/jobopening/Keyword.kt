package com.cheerha.crawler.jobopening

import jakarta.persistence.*

@Entity
@Table(name = "keyword")
data class Keyword(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 50, nullable = false)
    val name: String
)
