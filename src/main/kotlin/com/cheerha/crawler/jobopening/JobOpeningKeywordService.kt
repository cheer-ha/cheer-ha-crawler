package com.cheerha.crawler.jobopening

import org.springframework.stereotype.Service

@Service
class JobOpeningKeywordService(
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
) {

    fun saveKeywordList(
        skills: List<String>,
        jobOpening: JobOpening
    ) {
        if (skills.isEmpty()) return

        // 소문자로 변환하여 중복 제거한 리스트 생성
        val normalizedSkills = skills.map { it.lowercase() }.toSet()

        // 데이터베이스에서 기존 키워드 조회 (소문자 기준)
        val existingKeywords = keywordRepository.findByNameIn(normalizedSkills)
            .associateBy { it.name.lowercase() } // 기존 값 맵핑 (소문자 기준)

        for (skill in normalizedSkills) {
            if (skill.isNotBlank()) {
                // 기존 키워드가 있으면 원래 저장된 값 사용, 없으면 새로운 키워드 저장
                val keyword = existingKeywords[skill] ?: keywordRepository.save(Keyword(name = skill))

                // JobOpening과 키워드 연결 (중복 저장 방지)
                if (!jobOpeningKeywordRepository.existsByJobOpeningAndKeyword(jobOpening, keyword)) {
                    jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
                }
            }
        }
    }
}