package com.chsoft.testng.cs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.testng.asserts.SoftAssert;
import static org.testng.Assert.assertEquals;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import alarm.EventLevel;
import alarm.EventStatus;
import alarm.EventType;
import alarm.ThresholdType;
import browser.ConstantKeyValue;
import browser.mockdata.BrowserDataBean;
import util.CellKey;
import util.ConfigCell;
import com.xiaoleilu.hutool.db.Entity;


public class BrowserTestProcess{

	Logger logger = LoggerFactory.getLogger(BrowserTestProcess.class);
	private LinkedBlockingDeque<BrowserDataBean> assertsDeque = new LinkedBlockingDeque<>();
	ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
	CompletionService<Map<ExpectPerfData,List<Entity>>> completionService = new ExecutorCompletionService<>(fixedThreadPool);
	
	ExecutorService logThreadPool = Executors.newFixedThreadPool(10);
	CompletionService<HashBasedTable<ExpectPerfData,String,List<String>>> logCompletionService = new ExecutorCompletionService<>(logThreadPool);
	
	SoftAssert softAssert = new SoftAssert();
	int maxRetry = 3;
	float delta = 0.01f;
	long maxWait = 6 * 60 * 1000;
	
	public String logFilePath;
	public String logFilePrefix;
	public Date startTime;//记录需要触发警报的历史值,用于after之后进行比较
	public String targetParentName;//应用名称
	
	public LinkedBlockingDeque<BrowserDataBean> getAssertsDeque() {
		return assertsDeque;
	}

	public void addAssertsToDeque(BrowserDataBean data) {
		try {
			this.assertsDeque.put(data);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	//event_type  0 - 无 1 - 指标超过阈值 9 - 触发报警通知 10 - 解除报警通知'
	//event_level 事件级别：0 - 无 1 - 警告 2 - 严重
	//status      0解除 1触发 2手动解除 3无数据解除,4-临时删除
	//begin_time  事件开始时间
	//end_time    事件结束时间
	/**
	 * 1: 触发告警 产生一条记录
	 * 	  1.1: begin_time=event_time,end_time=null,event_type=1,event_level=1,status=1
	 * 2: 解除告警,同时升级严重 告警记录
	 *    2.1: 告警历史记录update: end_time=event_time,status=0
	 *    2.2: 严重insert: begin_time=event_time,end_time=null,event_type=1,event_level=2,status=1
	 *    2.3: 触发警报通知insert:begin_time=event_time,end_time=null,event_type=9,event_level=2,status=1
	 * 3: 解除严重:
	 *    3.1: 严重历史记录update: end_time=event_time,status=0
	 *    3.2: 解除报警通知insert:begin_time=event_time,end_time=null,event_type=10,event_level=2,status=0
	 */
	public void executeAfterTest(){
		List<ExpectPerfData> perfDataList = Lists.newArrayList();
		while(!assertsDeque.isEmpty()){
			BrowserDataBean data = assertsDeque.poll();
			
			logger.info("执行extract browser任务至 perfDataList,队列中剩余: "+assertsDeque.size());
			
			int browserAppId = data.browserAppId;
			HashMap<CellKey, ConfigCell> asserts = data.getAsserts();
			
			asserts.forEach((cellKey,configCell)->{
				getInitPerfData(configCell).forEach(perfData->{
					perfData.setStartTime(startTime);
					perfData.setTargetParentId(browserAppId);
					
					HashMap<String, Number> dbValue = configCell.getDbValue();
	    			String metricKey = cellKey.getKey();
	    			byte metricId = ConstantKeyValue.METRIC_NAME_ID.get(metricKey);
	    			int targetType = cellKey.getTargetType();
		    		perfData.setCount(dbValue.getOrDefault("count",-1).intValue());//当前触发阈值
		    		perfData.setMetricId(metricId);
		    		perfData.setTargetType((byte)targetType);
		    		perfData.setReleaseThreshold(dbValue.getOrDefault("release_threshold",0).floatValue());//解除阈值
		    		perfData.setReleaseValue(dbValue.getOrDefault("release_value",0).floatValue());//解除平均值
		    		perfData.setThresholdType(dbValue.getOrDefault("threshold_type",ThresholdType.STATIC).byteValue());//动静态
		    		perfData.setValue(dbValue.getOrDefault("value",-1).floatValue());//触发的平均吞吐率
		    		perfData.setSendEmail(configCell.isSendEmail());
		    		perfData.setSendMobile(configCell.isSendMobile());
		    		perfData.setSendThrid(configCell.isSendThrid());
		    		perfData.setSendWeiXin(configCell.isSendWeiXin());
		    		perfDataList.add(perfData);
				});
			});
		}
		
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = LocalDateTime.ofInstant(startTime.toInstant(),ZoneId.systemDefault());
		while(Duration.between(start, end).toMillis() <= maxWait){//这里必须满足从数据上传到开始执行任务大于5分钟,数据库中有些延迟
			logger.info("距离执行任务，剩余 {} 秒 ", ((maxWait-Duration.between(start, end).toMillis()))/1000);
			sleepSeconds(15);
			end = LocalDateTime.now();
		}
		
		logger.info("开始执行任务，共执行 {} 个任务: ",perfDataList.size());
		
		perfDataList.forEach(perfData -> {
			completionService.submit(expectDbAsync(perfData));//数据库异步提交
			logCompletionService.submit(expectLogAsync(perfData));//日志异步提交
		});
		
		expectDbCallBack(perfDataList);//数据库回调
		expectLogCallBack(perfDataList);//日志回调
		
		softAssert.assertAll();
		
		fixedThreadPool.shutdown();
		logThreadPool.shutdown();
		try {
			fixedThreadPool.awaitTermination(10, TimeUnit.MINUTES);
			logThreadPool.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	public Callable<Map<ExpectPerfData,List<Entity>>> expectDbAsync(ExpectPerfData perfData){
		Callable<Map<ExpectPerfData,List<Entity>>> task = new Callable<Map<ExpectPerfData,List<Entity>>>() {
			@Override
			public Map<ExpectPerfData,List<Entity>> call() throws Exception {
				Map<ExpectPerfData,List<Entity>> result = Maps.newHashMap(); // 比对db
				List<Entity> queryEvent = Lists.newArrayList(); 
				int retry = 0;
				while (result.size()==0 && ((retry++) < maxRetry)) {
					queryEvent = DbExtract.queryEvent(perfData);
					sleepSeconds(5);
				}
				result.put(perfData, queryEvent);
				return result;
			}
		};
		return task;
	}
	
	public void expectDbCallBack(List<ExpectPerfData> perfDataList){
		perfDataList.forEach(data -> {
			try {
				Map<ExpectPerfData,List<Entity>> result = completionService.take().get();
				result.forEach((perfData,event)->{
					if (!perfData.isEventTrigger()) {
						softAssert.assertEquals(event.size(), 0, String.format("数据库查出异常纪录,期望0条记录,实际为%d条"+perfData, event.size()));
						return;
					}
					softAssert.assertEquals(event.size(), 1, String.format("数据库查询不到此数据,期望1条记录,实际为%d条"+perfData, event.size()));
					if( event.size() > 0){
						Entity entity = event.get(0);
						float releaseThreshold = entity.getFloat("release_threshold");
						float releaseValue = entity.getFloat("release_value");
						int count = entity.getInt("count");
						float value = entity.getFloat("value");
						
						softAssert.assertEquals(releaseThreshold, perfData.getReleaseThreshold(), delta, String.format("数据库查询releaseThreshold,期望%f,实际%f", perfData.getReleaseThreshold(), releaseThreshold));
						
						softAssert.assertEquals(releaseValue, perfData.getReleaseValue(), delta, String.format("数据库查询releaseValue,期望%f,实际%f", perfData.getReleaseValue(), releaseValue));
						
						softAssert.assertEquals(count, perfData.getCount(), String.format("数据库查询count,期望%d,实际%d", perfData.getCount(), count));
						
						softAssert.assertEquals(value, perfData.getValue(), delta, String.format("数据库查询value,期望%f,实际%f", perfData.getValue(), value));
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		});
	}

	
	/**
	 * 数据库抽取
	 * @param perfData
	 */
	@Deprecated
	public void expectDbSync(ExpectPerfData perfData){
		Future<?> submit = fixedThreadPool.submit(() -> {
			List<Entity> queryEvent = Lists.newArrayList(); // 比对db
			int retry = 0;
			while (CollectionUtils.isEmpty(queryEvent) && ((retry++) < maxRetry)) {
				queryEvent = DbExtract.queryEvent(perfData);
				sleepSeconds(5);
			}
			if (!perfData.isEventTrigger()) {
				assertEquals(queryEvent.size(), 0, String.format("数据库查出异常纪录,期望0条记录,实际为%d条", queryEvent.size()));
				return;
			}

			assertEquals(queryEvent.size(), 1, String.format("数据库查询不到此数据,期望1条记录,实际为%d条", queryEvent.size()));
			
			Entity entity = queryEvent.get(0);
			float releaseThreshold = entity.getFloat("release_threshold");
			float releaseValue = entity.getFloat("release_value");
			int count = entity.getInt("count");
			float value = entity.getFloat("value");

			assertEquals(releaseThreshold, perfData.getReleaseThreshold(), delta, String.format("数据库查询releaseThreshold,期望%f,实际%f", perfData.getReleaseThreshold(), releaseThreshold));
			
			assertEquals(releaseValue, perfData.getReleaseValue(), delta, String.format("数据库查询releaseValue,期望%f,实际%f", perfData.getReleaseValue(), releaseValue));
			
			assertEquals(count, perfData.getCount(), String.format("数据库查询count,期望%d,实际%d", perfData.getCount(), count));
			
			assertEquals(value, perfData.getValue(), delta, String.format("数据库查询value,期望%f,实际%f", perfData.getValue(), value));
		});
		
		try {
			submit.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private void sleepSeconds(long timeout){
		try {
			TimeUnit.SECONDS.sleep(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	//ag -i '(browser)(.*告警)(.*apdex)' search.log
	public Callable<HashBasedTable<ExpectPerfData,String,List<String>>> expectLogAsync(ExpectPerfData perfData){
		Callable<HashBasedTable<ExpectPerfData,String,List<String>>> task = new Callable<HashBasedTable<ExpectPerfData,String,List<String>>>() {
			byte metricId = perfData.getMetricId();//指标名称
			byte targetType = perfData.getTargetType();//关键ajax,关键页面
			byte eventType = perfData.getEventType();//告警,解除
			String targetTypeName = String.format(ConstantKeyValue.TARGET_TYPE_NAME.get(targetType),eventType==EventType.NOTIFICATION ? "告警": "解除告警");//targetType 告警? 解除? 关键字
			String metricName = ConstantKeyValue.METRIC_ID_KEYWORD.get(metricId);//指标关键字
			String[] fields = new String[]{"isSendEmail","isSendMobile","isSendThrid","isSendWeiXin"};
			
			@Override
			public HashBasedTable<ExpectPerfData,String,List<String>> call() throws Exception {
				HashBasedTable<ExpectPerfData,String,List<String>> result = HashBasedTable.create();
				for (int i = 0; i < LOG_KEY_WORDS.size(); i++) {
					StringBuffer buffer = new StringBuffer(LOG_KEY_WORDS.get(i));
					buffer.append(",").append(targetTypeName).append(",").append(metricName);
					List<String> words = stringToList(buffer.toString());
					
					LocalDateTime startTime = LocalDateTime.ofInstant(perfData.getStartTime().toInstant(), ZoneId.systemDefault());
					LocalDateTime stopTime = LocalDateTime.ofInstant(perfData.getStopTime().toInstant(), ZoneId.systemDefault());
					List<String> extractLine = LogExtract.extractLineNew(startTime, stopTime, Paths.get(logFilePath), logFilePrefix, words);
					int retry = 0;
					while (extractLine.size()==0 && ((retry++) < maxRetry)) {
						extractLine = LogExtract.extractLineNew(startTime, stopTime, Paths.get(logFilePath), logFilePrefix, words);
						sleepSeconds(5);
					}
					result.put(perfData, fields[i], extractLine);
				}
				return result;
			}
		};
		return task;
	}
	
	
	public void expectLogCallBack(List<ExpectPerfData> perfDataList){
		perfDataList.forEach(data -> {
			try {
				HashBasedTable<ExpectPerfData, String, List<String>> result = logCompletionService.take().get();
				Map<String, List<String>> row = result.row(data);
				row.forEach((field,values)->{
					Method method = ReflectionUtils.findMethod(data.getClass(), field);
					Boolean isSend = (Boolean) ReflectionUtils.invokeMethod(method, data);
					if(!isSend){
						softAssert.assertEquals(values.size(), 0, String.format("查询 {%s} 日志记录,期望0条记录,实际为%d条,perfData:["+data+"]", field, values.size()));
						return;
					}
					softAssert.assertEquals(values.size(), 1, String.format("查询 {%s} 日志记录,期望1条记录,实际为%d条,perfData:["+data+"]", field, values.size()));
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		});
	}
	
	private List<String> stringToList(String keyword){
		List<String> result = Splitter.on(",").omitEmptyStrings().splitToList(keyword);
		return result;
	}
	
	/**
	 * 日志抽取
	 * @param perfData
	 */
	@Deprecated
	public void expectLogSync(ExpectPerfData perfData){
		logKeyWords(perfData).forEach((isSend,keywords)->{
			LocalDateTime startTime = LocalDateTime.ofInstant(perfData.getStartTime().toInstant(), ZoneId.systemDefault());
			LocalDateTime stopTime = LocalDateTime.ofInstant(perfData.getStopTime().toInstant(), ZoneId.systemDefault());
			try {
				List<String> extractLine = LogExtract.extractLine(startTime, stopTime, Paths.get(logFilePath), logFilePrefix, keywords);
				if(!isSend){
    				assertEquals(extractLine.size(), 0 ,String.format("日志文件查询到不期望的,期望0条记录,实际为%d条",extractLine.size()));
    				return;
    			}
				assertEquals(extractLine.size(), 1 ,String.format("日志文件查询,期望1条记录,实际为%d条",extractLine.size()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	@Deprecated
	static Map<String, String[]> IS_SEND_MAP = Maps.newHashMap();
	static List<String> LOG_KEY_WORDS = Lists.newArrayList();
	static{
		IS_SEND_MAP.put("isSendEmail", new String[]{"(sandbox)mail"});
		IS_SEND_MAP.put("isSendMobile", new String[]{"sms sended successfully"});
		IS_SEND_MAP.put("isSendThrid", new String[]{});
		IS_SEND_MAP.put("isSendWeiXin", new String[]{"wechat sended to"});
		
		LOG_KEY_WORDS.add("(sandbox)mail");//mail
		LOG_KEY_WORDS.add("sms sended successfully");//sms
		LOG_KEY_WORDS.add("");//thrid
		LOG_KEY_WORDS.add("wechat sended to");//weixin
	}
	
	@Deprecated
	private static Map<Boolean, String[]> logKeyWords(ExpectPerfData perfData){
		Map<Boolean, String[]> sendMap = Maps.newHashMap();
		IS_SEND_MAP.forEach((k,v)->{
			Method method = ReflectionUtils.findMethod(perfData.getClass(), k);
			Boolean isSend = (Boolean)ReflectionUtils.invokeMethod(method, perfData);
			sendMap.put(isSend,v);
		});
		return sendMap;
	}
	
	private static List<ExpectPerfData> getInitPerfData(ConfigCell cell) {
		List<ExpectPerfData> list = Lists.newArrayList();
		Multimap<String, ExpectPerfData> multiMap = ArrayListMultimap.create();
		multiMap.put("isAlertLow", new ExpectPerfData(EventType.VALUE_EXCEED_THRESHOLD, EventLevel.WARN, EventStatus.OPEN));
		multiMap.put("isAlertLowRelease", new ExpectPerfData(EventType.VALUE_EXCEED_THRESHOLD, EventLevel.WARN, EventStatus.CLOSED));

		multiMap.put("isAlertHigh", new ExpectPerfData(EventType.VALUE_EXCEED_THRESHOLD, EventLevel.CRITICAL, EventStatus.OPEN));
		multiMap.put("isAlertHighRelease", new ExpectPerfData(EventType.VALUE_EXCEED_THRESHOLD, EventLevel.CRITICAL, EventStatus.CLOSED));

		multiMap.put("isAlertHigh", new ExpectPerfData(EventType.NOTIFICATION, EventLevel.CRITICAL, EventStatus.OPEN));
		multiMap.put("isAlertHighRelease", new ExpectPerfData(EventType.NOTIFICATION_CLOSE, EventLevel.CRITICAL, EventStatus.OPEN));

		multiMap.keySet().stream().filter(field -> getBooleanField(cell, field) != null).map(field -> {
			Boolean trigger = getBooleanField(cell, field);
			multiMap.get(field).forEach(data -> {
				data.setEventTrigger(trigger);
			});
			return multiMap.get(field);
		}).forEach(list::addAll);
		return list;
	}

	
	public static Boolean getBooleanField(ConfigCell cell, String field) {
		Method method = ReflectionUtils.findMethod(cell.getClass(), field);
		Boolean isEventTrigger = (Boolean) ReflectionUtils.invokeMethod(method, cell);
		return isEventTrigger;
	}
	
	
}


