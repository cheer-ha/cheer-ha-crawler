package com.cheerha.crawler.crawler.saramin

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SaraminContentData(
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

        fun from(driver: WebDriver): SaraminContentData = SaraminContentData(
            company = driver.findElement(By.cssSelector("a.company")).getAttribute("title"),
            location = driver.findElement(By.xpath("//dt[contains(text(), '근무지역')]/following-sibling::dd")).text,
            employmentType = driver.findElements(By.xpath("//dt[contains(text(), '근무형태')]/following-sibling::dd//strong"))
                .joinToString(", ") { it.text },
            educationLevel = driver.findElement(By.xpath("//dt[contains(text(), '학력')]/following-sibling::dd//strong")).text,
            salary = run {
                driver.findElement(By.xpath("//dt[contains(text(), '급여')]/following-sibling::dd")).text.let { st ->
                    val n = Regex("\\d{1,3}(,\\d{3})*").find(st)?.value?.replace(",", "")?.toIntOrNull() ?: -1
                    if (n != -1) if (st.contains("연봉")) n else n * 12 else -1
                }
            },
            minExperienceYears = run {
                driver.findElement(By.xpath("//dt[contains(text(), '경력')]/following-sibling::dd//strong")).text.let { et ->
                    val nums = Regex("\\d+").findAll(et).map { it.value.toInt() }.toList()
                    when (nums.size) {
                        2 -> nums[0]
                        1 -> nums[0]
                        else -> 0
                    }
                }
            },
            maxExperienceYears = run {
                driver.findElement(By.xpath("//dt[contains(text(), '경력')]/following-sibling::dd//strong")).text.let { et ->
                    val nums = Regex("\\d+").findAll(et).map { it.value.toInt() }.toList()
                    when (nums.size) {
                        2 -> nums[1]
                        1 -> nums[0] + 3
                        else -> 3
                    }
                }
            },
            hiringStartAt = driver.findElement(By.xpath("//dt[contains(text(), '시작일')]/following-sibling::dd")).text
                .takeIf { it.isNotEmpty() }?.let { runCatching { LocalDateTime.parse(it, dateFormatter).atZone(zoneId) }.getOrNull() },
            hiringEndAt = driver.findElements(By.xpath("//dt[contains(text(), '마감일')]/following-sibling::dd")).firstOrNull()?.text
                ?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDateTime.parse(it, dateFormatter).atZone(zoneId) }.getOrNull() }
        )
    }
}
