package com.cheerha.crawler.jobopening

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface JobOpeningRepository : CrudRepository<JobOpening, Long> {
}