package com.cheerha.crawler.crawler.jobkorea

import com.cheerha.crawler.crawler.Crawler
import com.cheerha.crawler.driver.WebDriverFactory
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class JobKoreaCrawler(
    private val jobKoreaPage: JobKoreaPage,
    private val webDriverFactory: WebDriverFactory
) : Crawler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JobKoreaCrawler::class.java)
    }

    @Transactional
    override fun crawl(maxPages: Int) {
        val driver = webDriverFactory.createDriver()
        val wait = WebDriverWait(driver, Duration.ofSeconds(5))
        val baseUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_1"

        driver.get(baseUrl)
        try {
            JobKoreaFilters.apply(wait)

            (1..maxPages).forEach { currentPage ->
                //각 페이지마다 새로운 트랜잭션으로 처리
                jobKoreaPage.processPage(currentPage, driver, wait)
            }
        } catch (e: Exception) {
            log.error("크롤링 중 오류 발생: ${e.message}")
        } finally {
            driver.quit()
        }
    }
}
