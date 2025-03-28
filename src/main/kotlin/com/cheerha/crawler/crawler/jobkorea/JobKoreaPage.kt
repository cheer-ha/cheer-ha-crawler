package com.cheerha.crawler.crawler.jobkorea

import com.cheerha.crawler.jobopening.JobOpening
import com.cheerha.crawler.jobopening.JobOpeningKeywordService
import com.cheerha.crawler.jobopening.JobOpeningRepository
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Component
class JobKoreaPage(
    private val jobOpeningRepository: JobOpeningRepository,
    private val jobOpeningKeywordService: JobOpeningKeywordService
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JobKoreaCrawler::class.java)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processPage(currentPage: Int, driver: org.openqa.selenium.WebDriver, wait: WebDriverWait) {
        val pageUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_$currentPage"
        driver.get(pageUrl)
        log.info("현재 페이지: $currentPage")

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong a.link.normalLog")))
        val jobListings = driver.findElements(By.cssSelector("strong a.link.normalLog")).take(40)
        if (jobListings.isEmpty()) {
            log.warn("채용공고를 찾을 수 없으므로 크롤링 종료")
            return
        }

        jobListings.forEach { job ->
            processJob(job)
        }
    }

    fun processJob(job: WebElement) {
        val title = job.text
        val link = job.getAttribute("href")
        log.info("채용공고: $title ($link)")

        if (jobOpeningRepository.existsByJobOpeningUrl(link)) {
            log.info("이미 존재하는 채용공고: 건너뜀")
            return
        }

        Random.nextLong(500, 5000).also { delay ->
            log.info("⏳ 잡코리아 랜덤 대기 중: ${delay / 1000}초")
            Thread.sleep(delay)
        }

        val jobDoc = Jsoup.connect(link).get()
        val data = JobKoreaContentData.from(jobDoc)

        JobOpening.toEntity(
            title,
            data.company,
            data.location,
            data.salary,
            data.employmentType,
            data.educationLevel,
            link,
            data.experienceYears + 3,
            data.experienceYears,
            "개발자",
            data.hiringStartAt,
            data.hiringEndAt,
        ).also { jobOpening ->
            jobOpeningRepository.save(jobOpening)
            jobOpeningKeywordService.saveKeywordList(data.skills, jobOpening)
        }
    }
}