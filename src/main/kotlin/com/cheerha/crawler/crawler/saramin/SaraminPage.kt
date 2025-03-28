package com.cheerha.crawler.crawler.saramin

import com.cheerha.crawler.jobopening.JobOpening
import com.cheerha.crawler.jobopening.JobOpeningKeywordService
import com.cheerha.crawler.jobopening.JobOpeningRepository
import com.cheerha.crawler.normalization.AIHelper
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Component
class SaraminPage(
    private val jobOpeningRepository: JobOpeningRepository,
    private val jobOpeningKeywordService: JobOpeningKeywordService
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SaraminCrawler::class.java)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processPage(currentPage: Int, baseUrl: String, driver: WebDriver) {
        val pageUrl = "$baseUrl$currentPage"
        driver.get(pageUrl)
        SaraminScroller.toBottom(driver)
        log.info("현재 페이지: $pageUrl")

        val jobTitElements = driver.findElements(By.cssSelector(".job_tit"))
        if (jobTitElements.isEmpty()) {
            log.warn("채용공고를 찾을 수 없으므로 크롤링 종료")
            return
        }

        val jobTitles = jobTitElements.map { it.findElement(By.cssSelector("a.str_tit")).getAttribute("title") }
        val jobLinks = jobTitElements.map { it.findElement(By.cssSelector("a.str_tit")).getAttribute("href") }
        val jobMetaElements = driver.findElements(By.cssSelector(".job_meta"))
        val jobKeywords = jobMetaElements.map { meta ->
            meta.findElements(By.cssSelector("span"))
                .map { it.text.trim() }
                .filter { it.isNotEmpty() }
        }

        jobTitElements.indices.forEach { i ->
            processJob(i, jobTitles, jobLinks, jobKeywords, driver, baseUrl)
        }
    }

    fun processJob(
        i: Int,
        jobTitles: List<String>,
        jobLinks: List<String>,
        jobKeywords: List<List<String>>,
        driver: WebDriver,
        baseUrl: String
    ) {
        val title = jobTitles[i]
        val link = jobLinks[i]
        val rawKeywords = jobKeywords.getOrElse(i) { emptyList() }
        log.info("채용공고: $title ($link)")

        if (jobOpeningRepository.existsByJobOpeningUrl(link)) {
            log.info("이미 존재하는 채용공고: 건너뜀")
            return
        }

        Random.nextLong(500, 5000).also { delay ->
            log.info("⏳ 사람인 랜덤 대기 중: ${delay / 1000}초")
            Thread.sleep(delay)
        }

        //상세 페이지 파싱
        driver.get(link)
        Thread.sleep(2000)
        val data = SaraminContentData.from(driver)

        JobOpening.toEntity(
            title,
            data.company,
            data.location,
            data.salary,
            data.employmentType,
            data.educationLevel,
            link,
            data.maxExperienceYears,
            data.minExperienceYears,
            "개발자",
            data.hiringStartAt,
            data.hiringEndAt,
        ).also { jobOpening ->
            jobOpeningRepository.save(jobOpening)
            val normalizedKeywords = rawKeywords.map { keyword ->
                AIHelper.normalizeText(
                    keyword,
                    """
                    입력된 단어가 "기술 키워드"인지 판별해.
                    - 기술 키워드란 프로그래밍 언어, 데이터베이스, 프레임워크, 개발 관련 도구를 의미해.
                    - 예시: java, python, mysql, spring, docker, kafka, aws, c++, react, typescript, postgresql, tensorflow 등
                    - 기술 키워드가 아니면 "0"으로 변환해.
                    """.trimIndent()
                )
            }
            log.info("정형 데이터: $normalizedKeywords")
            jobOpeningKeywordService.saveKeywordList(normalizedKeywords, jobOpening)
        }

        //상세페이지가 인피니티 스크롤링이라 이 로직 필요함
        if (i == 50) {
            driver.get("${baseUrl}1")
        }
    }
}