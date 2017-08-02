package com.chsoft.testng.web.dao;

import java.util.List;

public interface BaseDao<T> {

	public List<T> list(String querySql);

	public T get(Class<T> clazz, String id);

	public void save(T entity);

	public void update(T entity);

}
