package com.cheerha.crawler.jobopening

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JobOpeningKeywordRepository : CrudRepository<JobOpeningKeyword, Long> {
    fun existsByJobOpeningAndKeyword(jobOpening: JobOpening, keyword: Keyword): Boolean
}