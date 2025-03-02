package com.cheerha.crawler.crawler

import com.cheerha.crawler.jobopening.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@Service
class JobKoreaCrawler(
    private val jobOpeningRepository: JobOpeningRepository,
    private val keywordRepository: KeywordRepository,
    private val jobOpeningKeywordRepository: JobOpeningKeywordRepository,
    private val webDriverFactory: WebDriverFactory
) : Crawler {

    @Transactional
    override fun crawl(maxPages: Int) {
        val driver = webDriverFactory.createDriver()

        var currentPage = 1

        //현재 페이지 URL 설정
        val pageUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_$currentPage"
        driver.get(pageUrl)

        try {
            //첫 페이지 로딩 대기 (5초)
            val wait = WebDriverWait(driver, Duration.ofSeconds(5))

            //"직무" 필터 클릭
            val jobButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("p.btn_tit"))
            )
            jobButton.click()
            Thread.sleep(3000)  //UI 대기 (3초)

            //"개발 / 데이터" 필터 클릭
            val mainJobElement = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step1_10031']"))
            )
            wait.until(ExpectedConditions.elementToBeClickable(mainJobElement)).click()
            Thread.sleep(3000) //UI 대기 (3초)

            //세부 직무에서 요소 체크
            val jobValues = listOf("1000229", "1000230", "1000231", "1000232") //백엔드, 프론트엔드, 웹, 앱 개발자
            for (jobValue in jobValues) {
                val jobElement = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for='duty_step2_$jobValue']"))
                )
                wait.until(ExpectedConditions.elementToBeClickable(jobElement)).click()
                Thread.sleep(1000) //UI 대기 (3초)
            }

            //"검색" 버튼 클릭
            val searchButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("dev-btn-search"))
            )
            searchButton.click()
            Thread.sleep(3000) //UI 대기 (3초)

            //정렬 기준 선택 "최신업데이트순"
            val orderSelect = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("orderTab"))
            )
            val select = Select(orderSelect)
            select.selectByValue("3") //최신업데이트순 value="3"
            Thread.sleep(3000) //UI 대기 (3초)

            while (currentPage <= maxPages) {
                val pageUrl = "https://www.jobkorea.co.kr/recruit/joblist?menucode=search#anchorGICnt_$currentPage"
                driver.get(pageUrl)
                println("현재 페이지: $currentPage")

                //페이지 로딩 대기
                WebDriverWait(driver, Duration.ofSeconds(5)).until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("strong a.link.normalLog"))
                )

                //해당 페이지의 채용공고 크롤링 (기본 정렬 개수인 40개만 가져옴)
                val jobListings = driver.findElements(By.cssSelector("strong a.link.normalLog")).take(40)
                if (jobListings.isEmpty()) {
                    println("채용공고를 찾을 수 없으므로 크롤링 종료")
                    break
                }

                for (job in jobListings) {
                    val title = job.text
                    val link = job.getAttribute("href")
                    println("채용공고: $title ($link)")

                    //랜덤 대기 (봇 탐지 방어)
                    val randomDelay = Random.nextLong(500, 5000)
                    println("⏳ 랜덤 대기 중: ${randomDelay / 1000}초")
                    Thread.sleep(randomDelay)

                    //Jsoup 으로 상세 페이지 크롤링
                    val jobDoc: Document = Jsoup.connect(link).get()

                    val company = jobDoc.select("span.coName").text()

                    //지역 (없으면 "미확인")
                    val locationElement = jobDoc.select("dt:contains(지역) + dd a")
                    val location = if (locationElement.isNotEmpty()) locationElement.text() else "미확인"

                    //고용형태 (<dd> 태그 안의 모든 <li> 요소를 , 로 분리해 가져옴)
                    val employmentType = jobDoc.select("dt:contains(고용형태) + dd ul.addList li strong").eachText().joinToString(", ")

                    val educationLevel = jobDoc.select("dt:contains(학력) + dd").text()

                    //일단 포지션은 개발자로 통일
                    val position = "개발자"

                    //급여 처리 (숫자가 없으면 -1)
                    val salaryText = jobDoc.select("dt:contains(급여) + dd").text()

                    val salary = if (salaryText.contains(Regex("[0-9]"))) {
                        //숫자 추출 (첫 번째 숫자만)
                        val firstNumber = Regex("\\d{1,3}(,\\d{3})*").find(salaryText)?.value
                            ?.replace(",", "") // 쉼표 제거
                            ?.toIntOrNull() ?: -1

                        //"연봉"이 포함되어 있으면 그대로, 아니면 12를 곱함
                        if (salaryText.contains("연봉")) firstNumber else firstNumber * 12
                    } else {
                        -1
                    }


                    //경력 (최소 / 최대 구분, 최대 = 최소 + 3)
                    val experienceText = jobDoc.select("dt:contains(경력) + dd span.tahoma").text()
                    val experienceYears = experienceText.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0

                    //채용 시작 & 마감일 (ZonedDateTime)
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                    val zoneId = ZoneId.of("Asia/Seoul")

                    val hiringStartText = jobDoc.select("dt:contains(시작일) + dd span.tahoma").text()
                    val hiringStartAt = runCatching {
                        LocalDate.parse(hiringStartText, dateFormatter).atStartOfDay(zoneId)
                    }.getOrNull()

                    val hiringEndText = jobDoc.select("dt:contains(마감일) + dd span.tahoma").text()
                    val hiringEndAt = runCatching {
                        LocalDate.parse(hiringEndText, dateFormatter).atStartOfDay(zoneId)
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
                        experienceYears + 3,
                        experienceYears,
                        position,
                        hiringStartAt,
                        hiringEndAt,
                    )
                    jobOpeningRepository.save(jobOpening)

                    //스킬 키워드 추출 및 저장
                    val skillElements = jobDoc.select("dt:contains(스킬) + dd")
                    val skills = skillElements.text().split(",").map { it.trim() }

                    for (skill in skills) {
                        if (skill.isNotBlank()) {
                            val existingKeyword = keywordRepository.findByName(skill)
                            val keyword = existingKeyword ?: keywordRepository.save(Keyword(name = skill))

                            jobOpeningKeywordRepository.save(JobOpeningKeyword(jobOpening = jobOpening, keyword = keyword))
                        }
                    }
                    println("DB 저장 완료: $title ($company), 스킬: $skills")
                }
                currentPage++
            }

        } catch (e: Exception) {
            println("크롤링 중 오류 발생: ${e.message}")
        } finally {
            driver.quit()
        }
    }
}
