package com.cheerha.crawler.crawler.saramin

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver

object SaraminScroller {

    fun toBottom(driver: WebDriver) {
        val js = driver as JavascriptExecutor
        var prevHeight = js.executeScript("return document.body.scrollHeight") as Long
        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);")
            Thread.sleep(2000)
            val newHeight = js.executeScript("return document.body.scrollHeight") as Long
            if (newHeight == prevHeight) break
            prevHeight = newHeight
        }
    }
}