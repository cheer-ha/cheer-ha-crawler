package com.cheerha.crawler.crawler.jobkorea

import com.cheerha.crawler.crawler.Crawler
import com.cheerha.crawler.driver.WebDriverFactory
import com.cheerha.crawler.jobopening.JobOpening
import com.cheerha.crawler.jobopening.JobOpeningKeywordService
import com.cheerha.crawler.jobopening.JobOpeningRepository
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import kotlin.random.Random

@Service
class JobKoreaCrawler(
    private val jobOpeningRepository: JobOpeningRepository,
    private val jobOpeningKeywordService: JobOpeningKeywordService,
    private val webDriverFactory: WebDriverFactory
) : Crawler {

    @Transactional
    override fun crawl(maxPages: Int) {
        val driver = webDriverFactory.createDriver()
        val wait = WebDriverWait(driver, Duration.ofSeconds(5))
        val baseUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_1"

        driver.get(baseUrl)
        try {
            applyJobFilters(wait)

            (1..maxPages).forEach { currentPage ->
                val pageUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_$currentPage"
                driver.get(pageUrl)
                println("현재 페이지: $currentPage")

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong a.link.normalLog")))
                val jobListings = driver.findElements(By.cssSelector("strong a.link.normalLog")).take(40)
                if (jobListings.isEmpty()) {
                    println("채용공고를 찾을 수 없으므로 크롤링 종료")
                    return@crawl
                }

                jobListings.forEach { job ->
                    val title = job.text
                    val link = job.getAttribute("href")
                    println("채용공고: $title ($link)")

                    //채용공고가 이미 존재하는지 먼저 확인
                    if (jobOpeningRepository.existsByJobOpeningUrl(link)) {
                        println("이미 존재하는 채용공고: 건너뜀")
                        return@forEach
                    }

                    //랜덤 대기 (봇 탐지 방어)
                    Random.nextLong(500, 5000)
                        .also { delay ->
                            println("⏳ 잡코리아 랜덤 대기 중: ${delay / 1000}초")
                            Thread.sleep(delay)
                        }

                    //Jsoup 으로 상세 페이지 크롤링
                    val jobDoc = Jsoup.connect(link).get()
                    val data = JobKoreaData.from(jobDoc)

                    //채용공고 저장
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
                        // 스킬 키워드 추출 및 저장
                        jobDoc.select("dt:contains(스킬) + dd").text()
                            .split(",")
                            .map(String::trim)
                            .let { skills ->
                                jobOpeningKeywordService.saveKeywordList(skills, jobOpening)
                            }
                    }
                }
            }
        } catch (e: Exception) {
            println("크롤링 중 오류 발생: ${e.message}")
        } finally {
            driver.quit()
        }
    }

    private fun applyJobFilters(wait: WebDriverWait) {
        //"직무" 필터 클릭
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("p.btn_tit")))
            .apply { click() }
            .also { Thread.sleep(3000) }

        //"개발 / 데이터" 필터 클릭
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step1_10031']")))
            .also { element ->
                wait.until(ExpectedConditions.elementToBeClickable(element)).click()
            }
            .also { Thread.sleep(3000) }

        //세부 직무에서 요소 체크
        listOf("1000229", "1000230", "1000231", "1000232").forEach { jobValue ->
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step2_$jobValue']")))
                .also { wait.until(ExpectedConditions.elementToBeClickable(it)).click() }
            Thread.sleep(1000)
        }

        //"검색" 버튼 클릭
        wait.until(ExpectedConditions.elementToBeClickable(By.id("dev-btn-search")))
            .apply { click() }
            .also { Thread.sleep(3000) }

        //정렬 기준 선택 "최신업데이트순"
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("orderTab")))
            .let { Select(it) }
            .apply { selectByValue("3") }
            .also { Thread.sleep(3000) }
    }
}