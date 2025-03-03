package com.cheerha.crawler.jobopening

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KeywordRepository : JpaRepository<Keyword, Long> {
    fun findByNameIn(names: Collection<String>): List<Keyword>
}
