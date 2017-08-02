package com.chsoft.testng.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
@ContextConfiguration(locations={"classpath:spring-context.xml"})
public class TestSpring extends AbstractTestNGSpringContextTests {

	@Autowired
	EmailGenerator emailGenerator;

	
	@Test
	void testEmailGenerator() {
		String email = emailGenerator.generate();
		
		System.out.println(email);

		Assert.assertNotNull(email);
		Assert.assertEquals(email, "feedback@yiibai.com");

	}
	

}
