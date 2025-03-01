plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.8.10"
	id("org.jetbrains.kotlin.plugin.allopen") version "1.8.10"
	id("org.jetbrains.kotlin.plugin.noarg") version "1.8.10"
}

group = "com.cheerha"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

allOpen {
	annotation("jakarta.persistence.Entity")
}
noArg {
	annotation("jakarta.persistence.Entity")
}

dependencies {
	//Spring Web
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	//mysql
	runtimeOnly("mysql:mysql-connector-java:8.0.33")

	//Redis (Redisson)
	implementation("org.redisson:redisson-spring-boot-starter:3.23.0")

	//AOP
	implementation("org.springframework.boot:spring-boot-starter-aop")

	//크롤링용
	implementation("org.jsoup:jsoup:1.15.3")
	implementation("org.seleniumhq.selenium:selenium-java:4.6.0")
	implementation("io.github.bonigarcia:webdrivermanager:5.8.0")

	//테스트
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
	testImplementation("io.mockk:mockk:1.13.7")

	//actuator
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	//batch
	implementation("org.springframework.boot:spring-boot-starter-batch")
	testImplementation("org.springframework.batch:spring-batch-test")

	//quartz(scheduler)
	implementation("org.springframework.boot:spring-boot-starter-quartz")

	//normalization
	implementation("com.squareup.okhttp3:okhttp:4.10.0")
	implementation("org.json:json:20230227")

	//dotenv
	implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

configurations.all {
	resolutionStrategy.eachDependency {
		if (requested.group == "org.seleniumhq.selenium" && requested.name.contains("selenium")) {
			useVersion("4.6.0")
		}
	}
}
