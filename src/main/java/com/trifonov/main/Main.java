package com.trifonov.main;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.GenericXmlApplicationContext;


import com.trifonov.compression.RelocatingCompressor;

public class Main {
	private final static Logger logger = LogManager.getLogger();
	private final static String THREAD = "thread";
	private final static String CYCLE = "cycle";
	
	public static void main(String[] args) {
		logger.info("APP IS STARTED.");
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load("classpath:spring/app-context.xml");
		ctx.refresh();
		RelocatingCompressor compressor = ctx.getBean("compressor", RelocatingCompressor.class);		
		
		
		Arrays.stream(args).forEach(arg -> {
			String[] argElements = arg.split("=");			
			if (THREAD.equalsIgnoreCase(argElements[0])) {
				try {
					int threadCount = Integer.parseInt(argElements[1]);
					if (threadCount > 0 && threadCount < 11) compressor.setThreadCount(threadCount);								
				} catch (NumberFormatException e) {
					logger.info("You incorrect set up thread count. Thread count has default value = {}", compressor.getThreadCount());			
				}
			} 
			if (CYCLE.equalsIgnoreCase(argElements[0])) {
				try {
					int compressionCycles = Integer.parseInt(argElements[1]);
					if (compressionCycles > 0 && compressionCycles < 16) compressor.setCompressionCycles(compressionCycles);									
				} catch (NumberFormatException e) {
					logger.info("You incorrect set up compression cycles. Compression cycles has default value = {}", compressor.getCompressionCycles());			
				}
			}
		});
		
		
		logger.info("thread count = {}", compressor.getThreadCount());
		logger.info("compression cycles = {}", compressor.getCompressionCycles());
		compressor.compress();
		ctx.close();
	}

}
