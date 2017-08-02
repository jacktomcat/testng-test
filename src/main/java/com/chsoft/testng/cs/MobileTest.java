package com.chsoft.testng.cs;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * 
 *	BeforeSuite -> BeforeTest -> BeforeClass -> Test -> AfterClass -> AfterTest -> AfterSuite
 */
public class MobileTest {

	@BeforeTest
	public void beforeTest() {
		System.out.println(this.getClass().getName()+": @BeforeTest");
	}
	
	@AfterTest
	public void afterTest() {
		System.out.println(this.getClass().getName()+": @AfterTest");
	}
	
	@BeforeClass
	@Parameters(value={"","",""})
	public void beforeClass() {
		System.out.println(this.getClass().getName()+": @BeforeClass");
	}
	
	@AfterClass
	public void afterClass() {
		System.out.println(this.getClass().getName()+": @AfterClass");
	}
	
	@BeforeSuite
	public void beforeSuite() {
		System.out.println(this.getClass().getName()+": @BeforeSuite");
	}
	
	@AfterSuite
	public void afterSuite() {
		System.out.println(this.getClass().getName()+": @AfterSuite");
	}
	
	/**
	 * 这里输入的数据,是从下面的@DataProvider中获取过来,且这个方法只能返回Object[][]
	 * @param message
	 */
	@Test(dataProvider="prepareData")
	public void testSend(UploadMessage message) {
		Assert.assertEquals(message.getName(), "zhangsan");
		System.out.println("@Test - testSend");
	}
	

	@DataProvider(name="prepareData")
	public Object[][] dataProvider() {
		Object[][] data = new Object[1][1];
		UploadMessage message = new UploadMessage();
		message.setName("zhangsan");
		data[0][0]= message;
		return data;
	}

}
