package com.trifonov.compression;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

/**
 * Класс для записи отчетов в файлы.
 * @author Alexandr Trifonov.
 *
 */
public class FileReportCreator {
	/**
	 * Имя папки с отчетами
	 */
	private String reportDir;
	
	private static final Logger logger = LogManager.getLogger();
	private final Clock clock = Clock.tickSeconds(ZoneId.systemDefault());
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
	private final ObjectMapper objMapper = new ObjectMapper();
	
	/**
	 * Path с абсолютным путем для размещения создаваемых отчетов.
	 */
	private Path reportPath;
	
		
	private void createReportPath() {
		Path currentPathAbs = Paths.get("").toAbsolutePath();
		logger.info("currentPathAbs = {}", currentPathAbs);
		reportPath = currentPathAbs.resolve(Paths.get(reportDir));
		if (!Files.exists(reportPath)) {
			try {
				Files.createDirectory(reportPath);
			} catch (IOException e) {
				logger.error("Error create reportPath ", e);
				reportPath = currentPathAbs;
			} catch (Exception e) {
				logger.error("Error create reportPath ", e);
				reportPath = currentPathAbs;
			}
		}
		logger.info("Report dir = {}", reportPath); 
	}
	
	public void writeReport(String report, String fileName) {
		logger.info("report = {}, filename = {}", report, fileName );
		LocalDateTime date = LocalDateTime.now(clock);
		Path path = reportPath.resolve(Paths.get(String.format("%s-%s", date.format(dateFormatter), fileName)));
		
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {				
			writer.write(report);		
		} catch (IOException e) {
			logger.error("Writing IOException. ", e);		
		} catch (Exception e) {
			logger.error("Writing exception. ", e);		
		}		
	}
	
	
	public void createFileInfoJson(Collection<FileInfo> files, String fileName) {
		logger.info("files = {}, fileName = {}", files, fileName);
		if (!files.isEmpty()) {
			LocalDateTime date = LocalDateTime.now(clock);
			Path path = reportPath.resolve(Paths.get(String.format("%s-%s", date.format(dateFormatter), fileName)));			
			try {
				objMapper.writeValue(path.toFile(), files);
			} catch (JsonGenerationException e) {
				logger.error("JsonGenerationException", e);	
			} catch (JsonMappingException e) {
				logger.error("JsonMappingException", e);	
			} catch (IOException e) {
				logger.error("JSON writing IOException", e);	
			}
		}
	}

	public String getReportDir() {
		return reportDir;
	}

	public void setReportDir(String reportDir) {
		this.reportDir = reportDir;
	}
	
	
}
