package com.free.coreservices.filefinder;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.free.coreservices.perfcounter.MetricGraphRrenderer;

@Controller
public class Grapher {
	@Autowired
	MetricGraphRrenderer render;
	
	@RequestMapping("/draw/**")
	public void graph(HttpServletRequest request,HttpServletResponse response) throws Exception {
		response.setHeader("Content-Type", "image/gif");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		
		Date startSecond = sdf.parse(request.getParameter("start"));
		Date endSecond= sdf.parse(request.getParameter("end"));
		
		String uri[]=request.getRequestURI().split("/");
		
		String bean = uri[uri.length-1];
		
		render.writeGraph(bean,response.getOutputStream(),startSecond.getTime()/1000,endSecond.getTime()/1000);
	}
	
}
