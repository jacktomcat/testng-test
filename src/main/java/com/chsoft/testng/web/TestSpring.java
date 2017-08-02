package com.chsoft.testng.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.chsoft.testng.web.entity.Browser;
import com.chsoft.testng.web.service.BrowserService;


@Test
@ContextConfiguration(locations={"classpath:spring-context.xml"})
public class TestSpring extends AbstractTestNGSpringContextTests {
	
	@Autowired
	BrowserService browserService;
	
	@Test
	void testDB() {
		List<Browser> list = browserService.getList();
		Assert.assertEquals(list.size(), 5);
	}
	

}
