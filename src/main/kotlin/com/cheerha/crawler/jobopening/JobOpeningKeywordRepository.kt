package com.cheerha.crawler.jobopening

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface JobOpeningKeywordRepository : CrudRepository<JobOpeningKeyword, Long>{
    @Query("SELECT COUNT(jk) > 0 FROM JobOpeningKeyword jk WHERE jk.jobOpening = :jobOpening AND jk.keyword = :keyword")
    fun existsByJobOpeningAndKeyword(@Param("jobOpening") jobOpening: JobOpening, @Param("keyword") keyword: Keyword): Boolean
}