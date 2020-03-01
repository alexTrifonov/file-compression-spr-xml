package com.trifonov.main;

import org.springframework.context.support.GenericXmlApplicationContext;

import com.trifonov.compression.Compressor;
import com.trifonov.compression.ImageFileVisitor;
import com.trifonov.compression.UltimateCompressor;

public class Main {

	public static void main(String[] args) {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load("classpath:spring/app-context.xml");
		ctx.refresh();
		ImageFileVisitor fileVisitor = ctx.getBean("fileVisitor", ImageFileVisitor.class);
		System.out.println("fileVisitor.getSourcePathName() = " + fileVisitor.getSourcePathName());
		System.out.println("fileVisitor.getTargetList() = " + fileVisitor.getTargetList());
		System.out.println("fileVisitor.getFailedList() = " + fileVisitor.getFailedReadFiles());
		
		UltimateCompressor compressor = ctx.getBean("compressor", UltimateCompressor.class);
		System.out.println("compressor.getImageFileVisitor() = " + compressor.getImageFileVisitor());
		System.out.println("compressor.getThreadCount() = " + compressor.getThreadCount());
		System.out.println("compressor.getTargetFiles() = " + compressor.getTargetFiles());
		System.out.println("compressor.getKeysFileName() = " + compressor.getKeysFileName());
		compressor.compress();
		ctx.close();
	}

}
