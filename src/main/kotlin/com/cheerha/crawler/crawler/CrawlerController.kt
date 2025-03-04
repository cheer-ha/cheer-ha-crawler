package com.cheerha.crawler.crawler

import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/crawler")
class CrawlerController(
    private val crawler: List<Crawler>
) {

    //동기 실행
    @GetMapping("/run-sync")
    fun startCrawling(@RequestParam(defaultValue = "1") pages: Int): String {
        val startTime = System.currentTimeMillis()
        crawler.forEach { crawler ->
            crawler.crawl(pages)
        }
        val totalTime = System.currentTimeMillis() - startTime
        return "동기 실행 완료! 총 소요 시간: ${totalTime}ms"
    }

    //비동기 멀티스레드 실행
    @GetMapping("/run-async")
    fun startCrawlingAsync(@RequestParam(defaultValue = "1") pages: Int): String {
        val startTime = System.currentTimeMillis()
        val futures = crawler.map { asyncCrawl(it, pages) }
        CompletableFuture.allOf(*futures.toTypedArray()).join() //전체 완료 대기
        val totalTime = System.currentTimeMillis() - startTime
        return "멀티스레드 실행 완료 총 소요시간: ${totalTime}ms"
    }

//    //비동기 코루틴 실행
//    @GetMapping("/run-coroutine")
//    suspend fun startCrawlingCoroutine(@RequestParam(defaultValue = "1") pages: Int): String {
//        val startTime = System.currentTimeMillis()
//        coroutineScope {
//            crawler.forEach { crawler ->
//                launch { crawler.crawl(pages) }
//            }
//        }
//        val totalTime = System.currentTimeMillis() - startTime
//        return "코루틴 실행 완료 총 소요시간: ${totalTime}ms"
//    }

    @Async
    fun asyncCrawl(crawler: Crawler, pages: Int): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            crawler.crawl(pages)
        }
    }

}
