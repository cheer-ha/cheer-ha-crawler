package com.cheerha.crawler.crawler.saramin

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SaraminData(
    val company: String,
    val location: String,
    val employmentType: String,
    val educationLevel: String,
    val salary: Int,
    val minExperienceYears: Int,
    val maxExperienceYears: Int,
    val hiringStartAt: ZonedDateTime?,
    val hiringEndAt: ZonedDateTime?
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
        private val zoneId = ZoneId.of("Asia/Seoul")

        fun from(driver: WebDriver): SaraminData {
            val company = driver.findElement(By.cssSelector("a.company")).getAttribute("title")
            val location = driver.findElement(By.xpath("//dt[contains(text(), '근무지역')]/following-sibling::dd")).text
            val employmentType = driver.findElements(By.xpath("//dt[contains(text(), '근무형태')]/following-sibling::dd//strong"))
                .joinToString(", ") { it.text }
            val educationLevel = driver.findElement(By.xpath("//dt[contains(text(), '학력')]/following-sibling::dd//strong")).text

            // 급여 처리
            val salaryText = driver.findElement(By.xpath("//dt[contains(text(), '급여')]/following-sibling::dd")).text
            val firstNumber = Regex("\\d{1,3}(,\\d{3})*")
                .find(salaryText)?.value
                ?.replace(",", "")
                ?.toIntOrNull() ?: -1
            val salary = if (firstNumber != -1) {
                if (salaryText.contains("연봉")) firstNumber else firstNumber * 12
            } else -1

            // 경력 처리 (최소 / 최대)
            val experienceText = driver.findElement(By.xpath("//dt[contains(text(), '경력')]/following-sibling::dd//strong")).text
            val experienceNumbers = Regex("\\d+").findAll(experienceText).map { it.value.toInt() }.toList()
            val (minExperience, maxExperience) = when (experienceNumbers.size) {
                2 -> experienceNumbers[0] to experienceNumbers[1]
                1 -> experienceNumbers[0] to (experienceNumbers[0] + 3)
                else -> 0 to 3
            }

            // 날짜 파싱
            val hiringStartText = driver.findElement(By.xpath("//dt[contains(text(), '시작일')]/following-sibling::dd")).text.takeIf { it.isNotEmpty() }
            val hiringStartAt = hiringStartText?.let {
                runCatching { LocalDateTime.parse(it, dateFormatter).atZone(zoneId) }.getOrNull()
            }
            val hiringEndElements = driver.findElements(By.xpath("//dt[contains(text(), '마감일')]/following-sibling::dd"))
            val hiringEndText = hiringEndElements.firstOrNull()?.text?.takeIf { it.isNotEmpty() }
            val hiringEndAt = hiringEndText?.let {
                runCatching { LocalDateTime.parse(it, dateFormatter).atZone(zoneId) }.getOrNull()
            }

            return SaraminData(
                company = company,
                location = location,
                employmentType = employmentType,
                educationLevel = educationLevel,
                salary = salary,
                minExperienceYears = minExperience,
                maxExperienceYears = maxExperience,
                hiringStartAt = hiringStartAt,
                hiringEndAt = hiringEndAt
            )
        }
    }
}