package com.cheerha.crawler.crawler.saramin

import com.cheerha.crawler.crawler.Crawler
import com.cheerha.crawler.driver.WebDriverFactory
import com.cheerha.crawler.jobopening.JobOpening
import com.cheerha.crawler.jobopening.JobOpeningKeywordService
import com.cheerha.crawler.jobopening.JobOpeningRepository
import com.cheerha.crawler.normalization.AIHelper
import org.openqa.selenium.By
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class SaraminCrawler(
    private val jobOpeningRepository: JobOpeningRepository,
    private val jobOpeningKeywordService: JobOpeningKeywordService,
    private val webDriverFactory: WebDriverFactory
) : Crawler {

    @Transactional
    override fun crawl(maxPages: Int) {
        val driver = webDriverFactory.createDriver()
        try {
            val baseUrl = "https://www.saramin.co.kr/zf_user/jobs/list/job-category?cat_kewd=84%2C86%2C87&sort=RD&page="

            (1..maxPages).forEach { currentPage ->
                driver.get("$baseUrl$currentPage")
                SaraminScroller.toBottom(driver)
                println("현재 페이지: ${baseUrl + currentPage}")

                val jobTitElements = driver.findElements(By.cssSelector(".job_tit"))
                if (jobTitElements.isEmpty()) {
                    println("채용공고를 찾을 수 없으므로 크롤링 종료")
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
                    val title = jobTitles[i]
                    val link = jobLinks[i]
                    val rawKeywords = jobKeywords.getOrElse(i) { emptyList() }

                    println("채용공고: $title ($link)")
                    if (jobOpeningRepository.existsByJobOpeningUrl(link)) {
                        println("이미 존재하는 채용공고: 건너뜀")
                        return@forEach
                    }

                    //랜덤 대기 (봇 탐지 방어)
                    Random.nextLong(500, 5000)
                        .also { delay ->
                            println("⏳ 사람인 랜덤 대기 중: ${delay / 1000}초")
                            Thread.sleep(delay)
                        }

                    //상세 페이지 파싱
                    driver.get(link)
                    Thread.sleep(2000)

                    val data = SaraminContentData.from(driver)
                    //채용공고 저장
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
                        //AIHelper를 통해 키워드 정규화 후 저장(
                        rawKeywords.map { keyword ->
                            AIHelper.normalizeText(
                                keyword,
                                """
                                입력된 단어가 "기술 키워드"인지 판별해.
                                - 기술 키워드란 프로그래밍 언어, 데이터베이스, 프레임워크, 개발 관련 도구를 의미해.
                                - 예시: java, python, mysql, spring, docker, kafka, aws, c++, react, typescript, postgresql, tensorflow 등
                                - 기술 키워드가 아니면 "0"으로 변환해.
                                """.trimIndent()
                            )
                        }.let { normalizedKeywords ->
                            println("정형 데이터: $normalizedKeywords")
                            jobOpeningKeywordService.saveKeywordList(normalizedKeywords, jobOpening)
                        }
                    }

                    //상세페이지가 인피니티 스크롤링이라 이 로직 필요함
                    if (i == 50) {
                        driver.get("$baseUrl${1}")
                    }
                }
            }
            println("크롤링 완료")
        } catch (e: Exception) {
            println("크롤링 중 오류 발생: ${e.message}")
        } finally {
            driver.quit()
        }
    }
}