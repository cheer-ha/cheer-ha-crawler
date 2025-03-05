package com.cheerha.crawler.crawler.jobkorea

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait

object JobKoreaFilters {
    fun apply(wait: WebDriverWait) {
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