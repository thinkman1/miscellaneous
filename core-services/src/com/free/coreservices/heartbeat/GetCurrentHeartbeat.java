package com.free.coreservices.heartbeat;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jpmc.dart.coreservices.util.heartbeat.JvmBean;
import com.jpmc.vpc.commons.xstream.VPCStream;

@Controller
public class GetCurrentHeartbeat {
	@Autowired
	FindAliveJvmsLocal findAliveJvmsLocal;
	
	@Autowired
	FindAliveJvmsFromOtherHosts findAliveJvmsFromOtherHosts;
	
	@RequestMapping("/HeartBeat/**")
	public void getHeartBeat(HttpServletResponse response) throws Exception {
		VPCStream.toXML(findAliveJvmsLocal.getAliveJvms(),response.getOutputStream());
	}
	
	@RequestMapping("/HeartBeatAll/**")
	public void getHeartBeatAll(HttpServletResponse response) throws Exception {
		List<JvmBean> jvms=findAliveJvmsLocal.getAliveJvms();
		jvms.addAll(findAliveJvmsFromOtherHosts.getAllRemoteServices());
		VPCStream.toXML(jvms,response.getOutputStream());
	}	
}
