package com.cheerha.crawler.jobopening

import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "job_opening")
@EntityListeners(AuditingEntityListener::class)
data class JobOpening(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 255, nullable = false)
    val title: String,

    @Column(length = 255, nullable = false)
    val company: String,

    @Column(length = 255, nullable = false)
    val location: String,

    val salary: Int,

    @Column(length = 20, nullable = false)
    val employmentType: String,

    @Column(length = 50, nullable = false)
    val educationLevel: String,

    @Column(length = 255, nullable = false)
    val jobOpeningUrl: String,

    @Column(nullable = true)
    val minExperienceYears: Int?,

    @Column(nullable = true)
    val maxExperienceYears: Int?,

    @Column(length = 255, nullable = false)
    val position: String,

    val hiringStartAt: ZonedDateTime?,

    val hiringEndAt: ZonedDateTime?,

    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    val viewCount: Int = 0,

    @OneToMany(mappedBy = "jobOpening", cascade = [CascadeType.ALL], orphanRemoval = true)
    val jobOpeningKeywordList: MutableList<JobOpeningKeyword> = mutableListOf()
)