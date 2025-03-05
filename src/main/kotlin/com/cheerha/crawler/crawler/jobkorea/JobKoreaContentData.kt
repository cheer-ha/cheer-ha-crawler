package com.cheerha.crawler.crawler.jobkorea

import org.jsoup.nodes.Document
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class JobKoreaContentData(
    val company: String,
    val location: String,
    val employmentType: String,
    val educationLevel: String,
    val salary: Int,
    val experienceYears: Int,
    val hiringStartAt: ZonedDateTime?,
    val hiringEndAt: ZonedDateTime?,
    val skills: List<String>
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val zoneId = ZoneId.of("Asia/Seoul")

        fun from(jobDoc: Document): JobKoreaContentData = JobKoreaContentData(
            company = jobDoc.select("span.coName").text(),
            location = jobDoc.select("dt:contains(지역) + dd a").text(),
            employmentType = jobDoc.select("dt:contains(고용형태) + dd ul.addList li strong")
                .eachText().joinToString(", "),
            educationLevel = jobDoc.select("dt:contains(학력) + dd").text(),
            salary = jobDoc.select("dt:contains(급여) + dd").text().let { salaryText ->
                val firstNumber = Regex("\\d{1,3}(,\\d{3})*")
                    .find(salaryText)?.value
                    ?.replace(",", "")
                    ?.toIntOrNull() ?: -1
                if (firstNumber != -1) if (salaryText.contains("연봉")) firstNumber else firstNumber * 12 else -1
            },
            experienceYears = jobDoc.select("dt:contains(경력) + dd span.tahoma").text()
                .replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0,
            hiringStartAt = jobDoc.select("dt:contains(시작일) + dd span.tahoma").text()
                .takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalDate.parse(it, dateFormatter).atStartOfDay(zoneId) }.getOrNull() },
            hiringEndAt = jobDoc.select("dt:contains(마감일) + dd span.tahoma").text()
                .takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalDate.parse(it, dateFormatter).atStartOfDay(zoneId) }.getOrNull() },
            skills = jobDoc.select("dt:contains(스킬) + dd").text()
                .split(",").map(String::trim).filter { it.isNotEmpty() }
        )
    }
}
