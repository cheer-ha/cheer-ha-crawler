package com.cheerha.crawler.crawler

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/crawler")
class CrawlerController(
    private val crawlerService: CrawlerService
) {

    @GetMapping("/run")
    fun startCrawling(@RequestParam(defaultValue = "1") pages: Int): String {
        crawlerService.jobKoreaCrawler(pages)
        return "크롤링이 시작되었습니다! (최대 $pages 페이지)"
    }
}
