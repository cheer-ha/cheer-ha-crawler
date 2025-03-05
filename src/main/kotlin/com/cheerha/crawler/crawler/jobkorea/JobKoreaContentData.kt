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

        fun from(jobDoc: Document): JobKoreaContentData {
            val company = jobDoc.select("span.coName").text()
            val location = jobDoc.select("dt:contains(지역) + dd a").text()
            val employmentType = jobDoc.select("dt:contains(고용형태) + dd ul.addList li strong")
                .eachText().joinToString(", ")
            val educationLevel = jobDoc.select("dt:contains(학력) + dd").text()

            val salaryText = jobDoc.select("dt:contains(급여) + dd").text()
            val firstNumber = Regex("\\d{1,3}(,\\d{3})*")
                .find(salaryText)?.value
                ?.replace(",", "")
                ?.toIntOrNull() ?: -1
            val salary = if (firstNumber != -1) {
                if (salaryText.contains("연봉")) firstNumber else firstNumber * 12
            } else -1

            val experienceYears = jobDoc.select("dt:contains(경력) + dd span.tahoma").text()
                .replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0

            val hiringStartAt = runCatching {
                LocalDate.parse(
                    jobDoc.select("dt:contains(시작일) + dd span.tahoma").text(),
                    dateFormatter
                ).atStartOfDay(zoneId)
            }.getOrNull()

            val hiringEndAt = runCatching {
                LocalDate.parse(
                    jobDoc.select("dt:contains(마감일) + dd span.tahoma").text(),
                    dateFormatter
                ).atStartOfDay(zoneId)
            }.getOrNull()

            val skills = jobDoc.select("dt:contains(스킬) + dd").text()
                .split(",")
                .map(String::trim)
                .filter { it.isNotEmpty() }

            return JobKoreaContentData(
                company = company,
                location = location,
                employmentType = employmentType,
                educationLevel = educationLevel,
                salary = salary,
                experienceYears = experienceYears,
                hiringStartAt = hiringStartAt,
                hiringEndAt = hiringEndAt,
                skills = skills
            )
        }
    }
}