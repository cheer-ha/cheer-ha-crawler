package com.cheerha.crawler.jobopening

import org.springframework.stereotype.Service
import java.util.*

@Service
class JobOpeningKeywordService(
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
) {

    fun saveKeywordList(
        skills: List<String>,
        jobOpening: JobOpening
    ) {
        for (skill in skills) {
            if (skill == "0"){
                return
            }
            if (skill.isNotBlank()) {
                val lowerSkill = skill.lowercase(Locale.getDefault())
                val existingKeyword = keywordRepository.findByName(lowerSkill)
                val keyword = existingKeyword ?: keywordRepository.save(Keyword(name = lowerSkill))

                jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
            }
        }
    }
}