package com.chsoft.testng.web.service.impl;

import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.chsoft.testng.web.dao.BaseDao;
import com.chsoft.testng.web.entity.Browser;
import com.chsoft.testng.web.service.BrowserService;


@Service
@Transactional
public class BrowserServiceImpl implements BrowserService{

	@Autowired
	private BaseDao<Browser> baseDao;

	@Override
	public List<Browser> getList() {
		return baseDao.list("from Browser");
	}
	
	
}
