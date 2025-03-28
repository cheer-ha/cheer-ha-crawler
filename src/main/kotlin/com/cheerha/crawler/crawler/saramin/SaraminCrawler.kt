package com.cheerha.crawler.crawler.saramin

import com.cheerha.crawler.crawler.Crawler
import com.cheerha.crawler.driver.WebDriverFactory
import com.cheerha.crawler.jobopening.JobOpening
import com.cheerha.crawler.jobopening.JobOpeningKeywordService
import com.cheerha.crawler.jobopening.JobOpeningRepository
import com.cheerha.crawler.normalization.AIHelper
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import kotlin.random.Random

@Service
class SaraminCrawler(
    private val saraminPage: SaraminPage,
    private val webDriverFactory: WebDriverFactory
) : Crawler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SaraminCrawler::class.java)
    }

    @Transactional
    override fun crawl(maxPages: Int) {
        val driver = webDriverFactory.createDriver()
        try {
            val baseUrl = "https://www.saramin.co.kr/zf_user/jobs/list/job-category?cat_kewd=84%2C86%2C87&sort=RD&page="
            (1..maxPages).forEach { currentPage ->
                //각 페이지마다 새로운 트랜잭션으로 처리
                saraminPage.processPage(currentPage, baseUrl, driver)
            }
            log.info("크롤링 완료")
        } catch (e: Exception) {
            log.error("크롤링 중 오류 발생: ${e.message}")
        } finally {
            driver.quit()
        }
    }
}
