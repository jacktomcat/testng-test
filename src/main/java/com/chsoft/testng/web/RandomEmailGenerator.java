package com.chsoft.testng.web;

import org.springframework.stereotype.Service;

@Service
public class RandomEmailGenerator implements EmailGenerator{

	public String generate() {
        return "feedback@yiibai.com";
    }
	
}
