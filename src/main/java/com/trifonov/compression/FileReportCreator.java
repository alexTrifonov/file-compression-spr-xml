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



/**
 * Класс для записи отчетов в текстовые файлы.
 * @author Alexandr Trifonov.
 *
 */
public class FileReportCreator {
	/**
	 * Имя папки с отчетами
	 */
	private String reportDir;
	
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Clock для получения текущих даты и времени.
	 */
	private final Clock clock = Clock.tickSeconds(ZoneId.systemDefault());
	
	/**
	 * DateTimeFormatter для форматирования даты.
	 */
	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");
	
	/**
	 * ObjectMapper для записи json-объектов в файлы.
	 */
	private final ObjectMapper objMapper = new ObjectMapper();
	
	/**
	 * Path с абсолютным путем для размещения создаваемых отчетов.
	 */
	private Path reportPath;
	
	//init-method	
	private void createReportPath() {
		Path currentPathAbs = Paths.get("").toAbsolutePath();		
		reportPath = currentPathAbs.resolve(Paths.get(reportDir));
		if (!Files.exists(reportPath)) {
			try {
				Files.createDirectory(reportPath);
			} catch (IOException e) {
				logger.error("Error creating reportPath ", e);
				reportPath = currentPathAbs;
			} 			
		}
		logger.info("Report dir = {}", reportPath); 
	}
	
	/**
	 * Метод для записи отчета в текстовый файл.
	 * @param report Текст отчета.
	 * @param fileName Имя файла.
	 */
	public void writeReport(String report, String fileName) {		
		LocalDateTime date = LocalDateTime.now(clock);
		Path path = reportPath.resolve(Paths.get(String.format("%s-%s", date.format(dateFormatter), fileName)));
		
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {				
			writer.write(report);		
		} catch (IOException e) {
			logger.error("WriteReport IOException. ", e);
			logger.info("Fail writing report. file = {}, report = {}", fileName, report);
		}
	}
	
	/**
	 * Метод для записи json-объектов в json-файлы.
	 * @param files Список объектов FileInfo.
	 * @param fileName Имя файла.
	 */
	public void createFileInfoJson(Collection<FileInfo> files, String fileName) {		
		if (!files.isEmpty()) {
			LocalDateTime date = LocalDateTime.now(clock);
			Path path = reportPath.resolve(Paths.get(String.format("%s-%s", date.format(dateFormatter), fileName)));			
			try {
				objMapper.writeValue(path.toFile(), files);
			} catch (JsonGenerationException e) {
				logger.error("JsonGenerationException ", e);
				logger.info("Fail writing json to file. file = {}, json = {}", fileName, files);
			} catch (JsonMappingException e) {
				logger.error("JsonMappingException ", e);
				logger.info("Fail writing json to file. file = {}, json = {}", fileName, files);
			} catch (IOException e) {
				logger.error("JSON writing IOException ", e);
				logger.info("Fail writing json to file. file = {}, json = {}", fileName, files);
			}
		}
	}
	
	/**
	 * Getter for ReportDir.
	 * @return ReportDir
	 */
	public String getReportDir() {
		return reportDir;
	}

	/**
	 * Setter for ReportDir.
	 * @param reportDir ReportDir
	 */
	public void setReportDir(String reportDir) {
		this.reportDir = reportDir;
	}
	
	
}
