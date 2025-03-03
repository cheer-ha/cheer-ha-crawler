package com.cheerha.crawler.jobopening

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class JobOpeningKeywordService(
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
) {

    @Transactional
    fun saveKeywordList(
        skills: List<String>,
        jobOpening: JobOpening
    ) {
        for (skill in skills) {
            if (skill.isNotBlank() && skill != "0" && !skill.contains("0") && !skill.contains("error")) {
                val lowerSkill = skill.lowercase(Locale.getDefault())
                val existingKeyword = keywordRepository.findByName(lowerSkill)
                val keyword = existingKeyword ?: keywordRepository.save(Keyword(name = lowerSkill))
                println("스킬 저장 됨: $keyword")
                jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
            }
        }
    }
}