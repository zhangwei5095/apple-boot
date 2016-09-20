package com.appleframework.boot.core.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jgroups.JChannel;
import org.jgroups.Message;

import com.appleframework.boot.core.Container;
import com.appleframework.boot.core.log4j.Log4jUtils;
import com.appleframework.boot.utils.Constants;
import com.appleframework.boot.utils.HttpUtils;
import com.appleframework.boot.utils.NetUtils;
import com.appleframework.boot.utils.SystemPropertiesUtils;
import com.appleframework.config.core.EnvConfigurer;

public class MonitorContainer implements Container {

	private static Logger logger = Logger.getLogger(MonitorContainer.class);
	
	private static long startTime = System.currentTimeMillis();
	
	private static String CONTAINER_NAME = "MonitorContainer";
	
	private static String MONITOR_URL = "http://monitor.appleframework.com/collect/application";

	@Override
	public void start() {
		logger.warn(CONTAINER_NAME + " start");
		startTime = System.currentTimeMillis();
		this.send();
	}

	@Override
	public void stop() {
		logger.warn(CONTAINER_NAME + " stop");
	}

	@Override
	public void restart() {
		this.send();
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String getName() {
		return CONTAINER_NAME;
	}

	@Override
	public String getType() {
		return CONTAINER_NAME;
	}
	
	private void send() {
		boolean isPostSuccess = postMessage();
		if(isPostSuccess == false) {
			sendMessage();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean postMessage() {
		try {
			logger.warn("发送监控同步数据通知");
			
			Properties prop = this.getMonitorProperties();
			Map<String, String> params = new HashMap<String, String>((Map)prop);
			HttpUtils.post(MONITOR_URL, params);
			return true;
		} catch (Exception e) {
			logger.error("通过httpclient发送监控同步数据通知失败");
			return false;
		}
	}
	
	private void sendMessage() {
		try {
			/**
			 * 参数里指定Channel使用的协议栈，如果是空的，则使用默认的协议栈，
			 * 位于JGroups包里的udp.xml。参数可以是一个以冒号分隔的字符串， 或是一个XML文件，在XML文件里定义协议栈。
			 */
			logger.warn("发送监控同步数据通知");

			Properties prop = this.getMonitorProperties();
			// 创建一个通道
			JChannel channel = new JChannel();
			// 加入一个群
			channel.connect("MonitorContainer");
			// 发送事件
			// 这里的Message的第一个参数是发送端地址
			// 第二个是接收端地址
			// 第三个是发送的字符串
			// 具体参见jgroup send API
			Message msg = new Message(null, null, prop);
			// 发送
			channel.send(msg);
			// 关闭通道
			channel.close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	private Properties getMonitorProperties(){
		Properties prop = SystemPropertiesUtils.getProp();
		String hostName = NetUtils.getLocalHost();
		List<String> runtimeParameters = this.getRuntimeParameters();
		prop.put("node.ip", NetUtils.getIpByHost(hostName));
		prop.put("node.host", hostName);
		prop.put("install.path", getInstallPath());
		prop.put("deploy.env", getDeployEnv());
		prop.put("log.level", Log4jUtils.getRootLoggerLevel().toString());
		prop.put("java.version", System.getProperty("java.version"));
		prop.put("start.param", runtimeParameters.toString());
		prop.put("mem.max", this.getRuntimeParameter(runtimeParameters, "-Xmx"));
		prop.put("mem.min", this.getRuntimeParameter(runtimeParameters, "-Xms"));
		return prop;
	}
	
	private String getInstallPath() {
		return System.getProperty("user.dir");
	}
	
	private String getDeployEnv() {
		String env = System.getProperty(Constants.KEY_DEPLOY_ENV);
		if (null == env) {
			env = System.getProperty(Constants.KEY_ENV);
			if (null == env) {
				env = EnvConfigurer.env;
				if (null == env) {
					env = SystemPropertiesUtils.getString(Constants.KEY_DEPLOY_ENV);
					if (null == env) {
						env = "UNKNOWN";
					}
				}
			}
		}
		return env;
	}

	public long getStartTime() {
		return startTime;
	}
	
	private List<String> getRuntimeParameters() {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		List<String> rList = new ArrayList<String>();
		List<String> aList = bean.getInputArguments();
		for (String key : aList) {
			if(key.indexOf("-X") > -1) {
				if(key.indexOf("bootclasspath") == -1) {
					rList.add(key);
				}
			}
		}
		return rList;
	}
	
	//解析启动参数JMX_MEM="-server -Xmx4g -Xms2g -Xmn512m -XX:PermSize=128m -Xss256k"
	private String getRuntimeParameter(List<String> runtimeParameters, String parameter) {
		String value = "UNKNOWN";
		for (String key : runtimeParameters) {
			if(key.indexOf(parameter) > -1) {
				value = key.substring(parameter.length());
			}
		}
		return value;
	}
	
}
