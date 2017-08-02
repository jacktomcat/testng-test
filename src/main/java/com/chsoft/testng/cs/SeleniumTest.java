package com.chsoft.testng.cs;

import java.io.File;
import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * 自动化测试
 * @author jacktomcat
 *
 */
public class SeleniumTest {
	
	
	
	private static ChromeDriverService service;
	private WebDriver driver;

	@BeforeClass
	public static void createAndStartService() throws IOException {
		System.setProperty("webdriver.chrome.driver", "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
		service = new ChromeDriverService.Builder().usingDriverExecutable(new File("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"))
				.usingAnyFreePort().build();
		service.start();
	}

	@AfterClass
	public static void createAndStopService() {
		service.stop();
	}

	@BeforeTest
	public void createDriver() {
		System.out.println(service.getUrl());
		driver = new RemoteWebDriver(service.getUrl(), DesiredCapabilities.chrome());
	}

	@AfterTest
	public void quitDriver() {
		driver.quit();
	}

	@Test(invocationCount=1)
	public void testGoogleSearch() {
		driver.get("https://www.baidu.com");
		WebElement searchBox = driver.findElement(By.id("kw"));
		searchBox.sendKeys("webdriver");
		searchBox.submit();
		Assert.assertEquals("webdriver - Google Search", driver.getTitle());
	}
	
}
