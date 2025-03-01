package com.cheerha.crawler.normalization

object SalaryNormalizer {
    fun normalizeSalary(salary: Int): Int {
        if(salary == -1) return 0

        val salaryStr = salary.toString()

        //0이 끝나는 지점까지만 추출
        val match = Regex("^([1-9]\\d*)0+").find(salaryStr)
        val extractedSalary = match?.groupValues?.get(1)?.toIntOrNull() ?: salary

        val finalSalary = extractedSalary * 10000  //만원 단위 변환

        return if (finalSalary <= 20000000) { //2천만 원 이하면 월급 기준이니 연봉으로 변환
            finalSalary * 12
        } else {
            finalSalary
        }
    }
}

