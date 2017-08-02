package com.chsoft.testng.web.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="browser")
public class Browser implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	@Id
	private String id;
	
	@Column(name="target_id")
	private String targetId;
	
	@Column(name="target_type")
	private String targetType;
	
	@Column(name="target_parent_id")
	private String targetParentId;
	
	@Column(name="metric_id")
	private String metricId;
	
	@Column(name="value")
	private String value;
	
	@Column(name="count")
	private String count;

	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getTargetParentId() {
		return targetParentId;
	}

	public void setTargetParentId(String targetParentId) {
		this.targetParentId = targetParentId;
	}

	public String getMetricId() {
		return metricId;
	}

	public void setMetricId(String metricId) {
		this.metricId = metricId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}

}
