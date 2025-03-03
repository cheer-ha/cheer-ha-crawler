package com.cheerha.crawler.jobopening

import org.springframework.stereotype.Service

@Service
class JobOpeningKeywordService(
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
) {

    public fun saveKeywordList(
        skills: List<String>,
        jobOpening: JobOpening
    ) {
        for (skill in skills) {
            if (skill.isNotBlank()) {
                val existingKeyword = keywordRepository.findByName(skill)
                val keyword = existingKeyword ?: keywordRepository.save(Keyword(name = skill))

                jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
            }
        }
    }
}