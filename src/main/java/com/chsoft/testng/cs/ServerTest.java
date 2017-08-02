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
public class ServerTest {

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
	 * 方法依赖测试,如果执行 testSend 失败, 那么这test方法将会跳过
	 * 
	 */
	@Test(dependsOnMethods={"testSend"})
	public void dependsMethods () throws InterruptedException {
		System.out.println("依赖testSend 的测试");
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
	
	/**
	 * TestNG预期异常测试
	 */
	@Test(expectedExceptions = ArithmeticException.class)
    public void divisionWithException() {
        int i = 1 / 0;
        System.out.println("After division the value of i is :"+ i);
    }
	
	
	/**
	 * TestNG忽略测试
	 * enabled=false 可以忽略这个测试方法
	 */
	@Test(enabled=false)
	public void disableTest() {
       System.out.println("=====忽略测试");
    }
	
	/**
	 * 超时测试
	 * @throws InterruptedException 
	 * 
	 */
	@Test(timeOut=2000)
	public void timeoutTest() throws InterruptedException {
		Thread.sleep(4000);
    }
	
	/**
	 * 自动化测试
	 * invocationCount 调用次数:10 
	 */
	@Test(invocationCount = 1000,threadPoolSize = 4)
	public void repeatThisTest() {
		 System.out.println("repeat This by Thread "+Thread.currentThread().getId() );
		 
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
