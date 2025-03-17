package com.cheerha.crawler.jobopening

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class JobOpeningKeywordService(
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JobOpeningKeywordService::class.java)
    }

    @Transactional
    fun saveKeywordList(
        skills: List<String>,
        jobOpening: JobOpening
    ) {
        skills.asSequence()
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() && it != "0" && !it.contains("0") && !it.contains("error") }
            .map { skill ->
                val keyword = keywordRepository.findByName(skill) ?: keywordRepository.save(Keyword(name = skill))
                if (!jobOpeningKeywordRepository.existsByJobOpeningAndKeyword(jobOpening, keyword)) {
                    jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
                    log.info("스킬 저장 됨: $keyword")
                }
            }
            .toList()
    }
}