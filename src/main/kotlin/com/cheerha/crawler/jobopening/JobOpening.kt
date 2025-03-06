package com.cheerha.crawler.jobopening

import com.cheerha.crawler.normalization.AIHelper
import jakarta.persistence.*
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "job_opening")
@EntityListeners(AuditingEntityListener::class)
data class JobOpening(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 255, nullable = false)
    val title: String,

    @Column(length = 255, nullable = false)
    val company: String,

    @Column(length = 255, nullable = false)
    val location: String,

    val salary: Int,

    @Column(length = 20, nullable = false)
    val employmentType: String,

    @Column(length = 50, nullable = false)
    val educationLevel: String,

    @Column(length = 255, nullable = false)
    val jobOpeningUrl: String,

    @Column(nullable = true)
    val minExperienceYears: Int?,

    @Column(nullable = true)
    val maxExperienceYears: Int?,

    @Column(length = 255, nullable = false)
    val position: String,

    val hiringStartAt: ZonedDateTime?,

    val hiringEndAt: ZonedDateTime?,

    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    val viewCount: Int = 0,

    @OneToMany(mappedBy = "jobOpening", cascade = [CascadeType.ALL], orphanRemoval = true)
    val jobOpeningKeywordList: MutableList<JobOpeningKeyword> = mutableListOf()
) {
    companion object {
        fun toEntity(
            title: String,
            company: String,
            location: String,
            salary: Int,
            employmentType: String,
            educationLevel: String,
            jobOpeningUrl: String,
            minExperienceYears: Int?,
            maxExperienceYears: Int?,
            position: String,
            hiringStartAt: ZonedDateTime?,
            hiringEndAt: ZonedDateTime?
        ): JobOpening {
            val normalizedCompany = AIHelper.normalizeText(company, "회사명이야. 주식회사를 나타내는 (주) 같은 건 빼줘. 굳이 한글화 할 필요는 없어. 영어면 영어 그대로 써줘")
            val normalizedLocation = AIHelper.normalizeText(location, "지역명이야. 쉼표로 단어를 분리해줘. '지도' 등 지역이름이 아닌 건 빼줘")
            val normalizedEmploymentType = AIHelper.normalizeText(employmentType, "고용 형태야. '정규직, 계약직, 아르바이트, 인턴, 프리랜서' 중 한 단어로만 나타내줘.")
            val normalizedEducationLevel = AIHelper.normalizeText(educationLevel, "학력이야. '무관, 고졸, 전문학사, 학사, 석사, 박사' 중 한 단어로만 나타내줘.")
            val normalizedPosition = AIHelper.normalizeText(position, "포지션이야. $title 을 이용해서, '백엔드 개발자, 프론트엔드 개발자, 개발자' 중 한 단어로만 나타내줘.")

            return JobOpening(
                title = title,
                company = normalizedCompany,
                location = normalizedLocation,
                salary = salary,
                employmentType = normalizedEmploymentType,
                educationLevel = normalizedEducationLevel,
                jobOpeningUrl = jobOpeningUrl,
                minExperienceYears = minExperienceYears,
                maxExperienceYears = maxExperienceYears,
                position = normalizedPosition,
                hiringStartAt = hiringStartAt,
                hiringEndAt = hiringEndAt
            )
        }
    }
}
