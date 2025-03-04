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
            val jobTitElements = driver.findElements(By.cssSelector(".job_tit"))
            if (jobTitElements.isEmpty()) {
                println("채용공고를 찾을 수 없으므로 크롤링 종료")
                break
            }

            val jobTitles = jobTitElements.map { it.findElement(By.cssSelector("a.str_tit")).getAttribute("title") }
            val jobLinks = jobTitElements.map { it.findElement(By.cssSelector("a.str_tit")).getAttribute("href") }

            val jobMetaElements = driver.findElements(By.cssSelector(".job_meta"))
            val jobKeywords = jobMetaElements.map { meta ->
                meta.findElements(By.cssSelector("span"))
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
            }

            for ((i) in jobTitElements.withIndex()) {
                val title = jobTitles[i]
                val link = jobLinks[i]
                val rawKeywords = jobKeywords[i]
                println("채용공고: $title ($link)")

                //채용공고가 이미 존재하는지 먼저 확인
                if (jobOpeningRepository.existsByJobOpeningUrl(link)) {
                    println("이미 존재하는 채용공고: 건너뜀")
                    continue
                }

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

                val hiringStartText = driver.findElement(By.xpath("//dt[contains(text(), '시작일')]/following-sibling::dd")).text.ifEmpty { null }
                val hiringStartAt = runCatching {
                    hiringStartText?.let { LocalDateTime.parse(it, dateFormatter).atZone(zoneId) }
                }.getOrNull()

                //마감일이 존재하지 않을 수도 있음
                val hiringEndElements = driver.findElements(By.xpath("//dt[contains(text(), '마감일')]/following-sibling::dd"))
                val hiringEndText = hiringEndElements.firstOrNull()?.text?.takeIf { it.isNotEmpty() }
                val hiringEndAt = hiringEndText?.let {
                    runCatching {
                        LocalDateTime.parse(it, dateFormatter).atZone(zoneId)
                    }.getOrNull()
                }

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

                println("비정형 데이터: $rawKeywords")
                //사람인은 자격요건 태그를 제공하지 않으므로 데이터 걸러내야함
                val keywords = rawKeywords.map { AIHelper.normalizeText(
                    it,
                    """
                    입력된 단어가 "기술 키워드"인지 판별해. 
                    - 기술 키워드란 프로그래밍 언어, 데이터베이스, 프레임워크, 개발 관련 도구를 의미해. 
                    - 예시: java, python, mysql, spring, docker, kafka, aws, c++, react, typescript, postgresql, tensorflow 등
                    - 기술 키워드가 아니면 "0"으로 변환해.
                    """.trimIndent()
                ) }
                println("정형 데이터: $keywords")

                jobOpeningKeywordService.saveKeywordList(keywords, jobOpening)

                //상세페이지가 인피니티 스크롤링이라 이 로직 필수임
                if(i == 50){
                    driver.get(baseUrl + 1)
                }
            }
        }
        println("크롤링 완료")
    }
}
