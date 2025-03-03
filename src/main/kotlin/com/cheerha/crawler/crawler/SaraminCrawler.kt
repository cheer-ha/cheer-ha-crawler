package com.cheerha.crawler.crawler

import com.cheerha.crawler.driver.WebDriverFactory
import com.cheerha.crawler.jobopening.*
import com.cheerha.crawler.normalization.AIHelper
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneId
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
        val baseUrl = "https://www.saramin.co.kr/zf_user/jobs/list/job-category?cat_kewd=84%2C86%2C87&sort=RD&page="

        for (currentPage in 1..maxPages) {
            driver.get(baseUrl + currentPage)

            val js = driver as JavascriptExecutor
            var prevHeight = js.executeScript("return document.body.scrollHeight") as Long

            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);") //페이지 끝까지 스크롤
                Thread.sleep(2000) //데이터 로딩 대기

                val newHeight = js.executeScript("return document.body.scrollHeight") as Long
                if (newHeight == prevHeight) break //더 이상 로드할 내용이 없으면 종료
                prevHeight = newHeight
            }

            val pageUrl = "$baseUrl$currentPage"
            println("현재 페이지: $pageUrl")

            //해당 페이지의 채용공고 크롤링
            val jobListings = driver.findElements(By.cssSelector(".job_tit a.str_tit"))
            if (jobListings.isEmpty()) {
                println("채용공고를 찾을 수 없으므로 크롤링 종료")
                break
            }

            val jobTitles = jobListings.map {
                it.getAttribute("title")
            }

            val jobLinks = jobListings.map {
                it.getAttribute("href")
            }
            for ((i) in jobListings.withIndex()) {
                val title = jobTitles[i]
                val link = jobLinks[i]
                println("채용공고: $title ($link)")

                //랜덤 대기 (봇 탐지 방어)
                val randomDelay = Random.nextLong(500, 5000)
                println("⏳ 사람인 랜덤 대기 중: ${randomDelay / 1000}초")
                Thread.sleep(randomDelay)

                //Jsoup 으로 상세 페이지 크롤링
                driver.get(link)
                Thread.sleep(2000)

                val company = driver.findElement(By.cssSelector("a.company")).getAttribute("title")

                //지역
                val location = driver.findElement(By.xpath("//dt[contains(text(), '근무지역')]/following-sibling::dd")).text

                //고용형태 (<dd> 태그 안의 모든 <li> 요소를 , 로 분리해 가져옴)
                val employmentType = driver.findElements(By.xpath("//dt[contains(text(), '근무형태')]/following-sibling::dd//strong"))
                    .joinToString(", ") { it.text }
                val educationLevel = driver.findElement(By.xpath("//dt[contains(text(), '학력')]/following-sibling::dd//strong")).text

                //일단 포지션은 개발자로 통일
                val position = "개발자"

                //급여 처리 (숫자가 없으면 -1)
                val salaryText = driver.findElement(By.xpath("//dt[contains(text(), '급여')]/following-sibling::dd")).text
                val firstNumber = Regex("\\d{1,3}(,\\d{3})*").find(salaryText)?.value
                    ?.replace(",", "")
                    ?.toIntOrNull() ?: -1
                val salary = if (firstNumber != -1) {
                    if (salaryText.contains("연봉")) firstNumber else firstNumber * 12
                } else -1

                //경력 (사람인은 최소 / 최대 둘 다 지원)
                val experienceText = driver.findElement(By.xpath("//dt[contains(text(), '경력')]/following-sibling::dd//strong")).text
                val experienceNumbers = Regex("\\d+").findAll(experienceText).map { it.value.toInt() }.toList()
                val (minExperience, maxExperience) = when (experienceNumbers.size) {
                    2 -> experienceNumbers[0] to experienceNumbers[1]
                    1 -> experienceNumbers[0] to (experienceNumbers[0] + 3)
                    else -> 0 to 3
                }

                //채용 시작 & 마감일 (ZonedDateTime)
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                val zoneId = ZoneId.of("Asia/Seoul")

                val hiringStartText = driver.findElement(By.xpath("//dt[contains(text(), '시작일')]/following-sibling::dd")).text
                val hiringStartAt = runCatching {
                    LocalDateTime.parse(hiringStartText, dateFormatter).atZone(zoneId)
                }.getOrNull()

                val hiringEndText = driver.findElement(By.xpath("//dt[contains(text(), '마감일')]/following-sibling::dd")).text
                val hiringEndAt = runCatching {
                    LocalDateTime.parse(hiringEndText, dateFormatter).atZone(zoneId)
                }.getOrNull()

                //채용공고 저장
                val jobOpening = JobOpening.toEntity(
                    title,
                    company,
                    location,
                    salary,
                    employmentType,
                    educationLevel,
                    link,
                    minExperience,
                    maxExperience,
                    position,
                    hiringStartAt,
                    hiringEndAt,
                )
                jobOpeningRepository.save(jobOpening)

//                val rawSkills = driver.findElements(By.cssSelector("div.tags ul.scroll li"))
//                    .map { it.text.trim() }
//                    .filter { it.isNotEmpty() }
//
//                //사람인은 자격요건 태그를 제공하지 않음
//                val skills = rawSkills.map { AIHelper.normalizeText(it, "영어가 아니라면 다 숫자 0으로 처리해") }
//                println("정형 데이터: $skills")
//
//                jobOpeningKeywordService.saveKeywordList(skills, jobOpening)
                if(i == 50){
                    driver.get(baseUrl + 1)
                }
            }
        }
        println("크롤링 완료")
    }
}
