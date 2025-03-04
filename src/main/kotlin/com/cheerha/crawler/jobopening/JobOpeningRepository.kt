package com.cheerha.crawler.jobopening

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobOpeningRepository : JpaRepository<JobOpening, Long> {
    fun existsByJobOpeningUrl(jobOpeningUrl: String): Boolean
}