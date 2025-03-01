package com.cheerha.crawler.jobopening

import jakarta.persistence.*

@Entity
@Table(
    name = "job_opening_keyword",
    uniqueConstraints = [UniqueConstraint(columnNames = ["keyword_id", "job_opening_id"])]
)
data class JobOpeningKeyword(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "keyword_id")
    val keyword: Keyword,

    @ManyToOne
    @JoinColumn(name = "job_opening_id")
    val jobOpening: JobOpening
)
