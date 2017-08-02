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
public class BrowserTest {

	@BeforeTest
	public void beforeTest() {
		System.out.println(this.getClass().getName()+": @BeforeTest");
	}
	
	@AfterTest
	public void afterTest() {
		System.out.println(this.getClass().getName()+": @AfterTest");
	}
	
	/**
	 * 从配置文件中读取参数
	 * @param dchost
	 * @param dbconfig
	 * @param poolsize
	 */
	@BeforeClass
	@Parameters(value={"dchost","dbconfig","poolsize"})
	public void beforeClass(String dchost,String dbconfig,String poolsize) {
		System.out.println("配置文件传递参数 {dchost:"+dchost+",dbconfig:"+dbconfig+",poolsize:"+poolsize+"}");
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
	
	
	@Test
	public void testInclude() {
		Assert.assertEquals("zhangsan", "zhangsan");
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
