package com.trifonov.compression;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tinify.AccountException;
import com.tinify.ClientException;
import com.tinify.ConnectionException;
import com.tinify.ServerException;
import com.tinify.Source;
import com.tinify.Tinify;

/*
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
*/
import lombok.Data;

@Data
public class UltimateCompressor implements Compressor {
	
	/**
	 * Количество потоков для сжатия файлов.
	 */
	private int threadCount = 10;
	private final Logger logger = LogManager.getLogger();
	
	//private String sourcePathName;
	private String keysFileName;
	
	private FileReportCreator reportCreator;
	
	/**
	 * Количество сжатых файлов.
	 */
	private AtomicInteger countCompressed;
	/**
	 * Очередь с файлами для сжатия.
	 */
	private Deque<FileInfo> targetFiles;
	/**
	 * Очередь с файлами, которые не удалось сжать.
	 */
	private Queue<FileInfo> failedCompressedFiles;
	/**
	 * Очередь с несжатыми файлами.
	 */
	private Queue<FileInfo> uncompressedFiles;
	/**
	 * Список с файлами к которым не был получен доступ.
	 */
	//private List<FileInfo> failedReadFilesList;
	/**
	 * Очередь с битыми ключами.
	 */
	private Queue<String> failedKeys;
	/**
	 * Очередь с израсходованными ключами.
	 */
	private Queue<String> wasteKeys;
	/**
	 * Очередь с неиспользованными до лимита ключами.
	 */
	private Queue<String> incompleteKeys;		
	
	private ImageFileVisitor imageFileVisitor;
	
	private Path compressedFileDirectory;
	private Path currentPathAbs;
	
	private void init() {
		countCompressed = new AtomicInteger(0);
		targetFiles = new ConcurrentLinkedDeque<>(imageFileVisitor.getTargetList());
		failedCompressedFiles = new ConcurrentLinkedQueue<>();
		uncompressedFiles = new ConcurrentLinkedQueue<>();
		failedKeys = new ConcurrentLinkedQueue<>();
		wasteKeys = new ConcurrentLinkedQueue<>();
		incompleteKeys = new ConcurrentLinkedQueue<>();
		currentPathAbs = Paths.get("").toAbsolutePath();
		compressedFileDirectory = currentPathAbs.resolve(Paths.get("compressed-files"));
		logger.info("compressedFileDirectory = {}", compressedFileDirectory.toString());
		if (!Files.exists(compressedFileDirectory)) {
			try {
				Files.createDirectories(compressedFileDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
	
	@Override
	public void compress() {		
		
		//ConcurrentLinkedQueue<String> keys = new ConcurrentLinkedQueue<>(getKeys(keysFileName));
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		targetFiles = targetFiles.stream().sorted(Comparator.comparing(FileInfo::getSize).reversed()).collect(Collectors.toCollection(ConcurrentLinkedDeque::new));
		
		
		while (!targetFiles.isEmpty()) {			
			int countLastFiles = targetFiles.size();
			System.out.printf("%d files left%n", countLastFiles);
			List<String> keys = getKeys(keysFileName);
			
			boolean keysIsEmpty = keys.isEmpty();
			
			while(keysIsEmpty) {
				System.out.printf("Need %d keys%n", countLastFiles/500 + 1);
				
				try {
					WatchService watchService = FileSystems.getDefault().newWatchService();
					Path path = Paths.get("D:\\trifonov\\tinify");			
					path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
					WatchKey key;
					try {				
						boolean addedKeys = false;
						while(!addedKeys) {
							key = watchService.take();
							for(WatchEvent<?> event : key.pollEvents()) {
								WatchEvent<Path> ev = cast(event);
								Path eventPath = ev.context();						
								
								if ("keys.txt".equals(eventPath.toString())) {							
									System.out.println("File keys.txt is modified.");
									addedKeys = true;
									break;
								}
							}			
							key.reset();
						}				
					} catch (InterruptedException e) {				
						e.printStackTrace();
					}			
					watchService.close();
				} catch (IOException e) {			
					e.printStackTrace();
				}
				
				keys = getKeys(keysFileName);
				keysIsEmpty = keys.isEmpty();
				
			}			
			compressionRun(keys, pool);
			
			
		}
		
		
		logger.info("Compression is finished. Compressions count = " + countCompressed);
		
		pool.shutdown();
		while(!pool.isTerminated()) {}
		
		if (!targetFiles.isEmpty()) {
			uncompressedFiles.addAll(targetFiles);
		}
	}
	
	
	private void compressionRun(List<String> keys, ExecutorService pool) {
		
		Iterator<String> keyIterator = keys.iterator();	
		while(keyIterator.hasNext()) {
			String key = keyIterator.next();
			pool.submit(() -> {
				Tinify.setKey(key);
				boolean validKeyAndHasFile = true;
				while(validKeyAndHasFile) {
					if (!targetFiles.isEmpty()) {				
						FileInfo fileInfo = targetFiles.poll();
						try {
							Source  source = Tinify.fromFile(fileInfo.getName());							
							source.toFile(fileInfo.getName());							
							countCompressed.incrementAndGet();
							logger.info("Compressed file = {}, old size = {}", fileInfo.getName(),  fileInfo.getSize());
						} catch (AccountException e) {
							wasteKeys.add(key);							
							//uncompressedFiles.add(fileInfo);				
							targetFiles.addFirst(fileInfo);							
							validKeyAndHasFile = false;
							logger.error("AccountException, message = {}, key = {}, file = {}, size = {}", e.getMessage(), key, fileInfo.getName(), fileInfo.getSize(), e);
						} catch (ClientException e) {
							failedCompressedFiles.add(fileInfo);
							logger.error("ClientException, message = {}, key = {}, file = {}, size = {}", e.getMessage(), key, fileInfo.getName(), fileInfo.getSize(), e);
						} catch (ServerException e) {
							uncompressedFiles.add(fileInfo);							
							logger.error("ServerException, message = {}, key = {}, file = {}, size = {}", e.getMessage(), key, fileInfo.getName(), fileInfo.getSize(), e);
						} catch (ConnectionException e) {
							//uncompressedFiles.add(fileInfo);
							targetFiles.addFirst(fileInfo);
							logger.error("ConnectionException, message = {}, key = {}, file = {}, size = {}", e.getMessage(), key, fileInfo.getName(), fileInfo.getSize(), e);
						} catch (java.lang.Exception e) {
							uncompressedFiles.add(fileInfo);
							logger.error("java.lang.Exception, message = {}, key = {}, file = {}, size = {}", e.getMessage(), key, fileInfo.getName(), fileInfo.getSize(), e);
						}						
					} else {
						incompleteKeys.add(key);
						validKeyAndHasFile = false;
						logger.info("Files are finished. key = {}", key);
					}	
				}
			});			
		}
	}
	
	
	/**
	 * Метод для получения списка ключей.
	 * @param keysPath Файл с ключами.
	 * @return
	 */
	private List<String> getKeys(String keysPathName) {
		List<String> keys = new LinkedList<>();
		try {
			Path currentPathAbs = Paths.get("").toAbsolutePath();
			
			//Path keysPath = Paths.get(keysPathName);
			Path keysPath = currentPathAbs.resolve(keysFileName);
			keys = new LinkedList<>(Files.readAllLines(keysPath, Charset.forName("UTF-8")));
			keys.removeIf(str -> str.isEmpty());
			logger.info("keys: {}", keys);
		} catch (IOException e) {
			logger.error("Failed read keys list. IOException. ", e);			
		} catch (Exception e) {
			logger.error("Failed read keys list. Exception. ", e);			
		}		
		return keys;
	}
	
	private void compressionReport() {
		
		String lineSeparator = System.lineSeparator();
		
		Map<String, Collection<FileInfo>> mapReportFileName = new HashMap<>();
		mapReportFileName.put("failed-compressed-files.txt", failedCompressedFiles);
		mapReportFileName.put("failed-read-files.txt", imageFileVisitor.getFailedReadFiles());
		mapReportFileName.put("uncompressed-files.txt", uncompressedFiles);
		
		Map<String, Collection<FileInfo>> mapReportFileJson = new HashMap<>();
		mapReportFileJson.put("failed-compressed-files.json", failedCompressedFiles);
		mapReportFileJson.put("failed-read-files.json", imageFileVisitor.getFailedReadFiles()); 
		mapReportFileJson.put("uncompressed-files.json", uncompressedFiles);
		
		Map<String, Queue<String>> mapReportKey = new HashMap<>();
		mapReportKey.put("failed-keys.txt", failedKeys);
	
		
		mapReportKey.put("incomplete-keys.txt", incompleteKeys);
		mapReportKey.put("waste-keys.txt", wasteKeys);
		//reportCreator.createReportNumber(countCompressed.get(),	);
		reportCreator.writeReport(countCompressed.toString(), "count-compressed.txt");
		
		mapReportFileName.entrySet().forEach(e -> {
			if (!e.getValue().isEmpty()) {
				String report =   e.getValue().stream().map(FileInfo::getName).map(x -> x + lineSeparator).reduce("", String::concat).trim();
				//e.getValue().stream().map(FileInfo::getName).map(x -> x + lineSeparator);
				reportCreator.writeReport(e.getKey(), report);
			} 
		});
		
		mapReportKey.entrySet().forEach(e -> {
			if (!e.getValue().isEmpty()) {		
				String report =   e.getValue().stream().map(x -> x + lineSeparator).reduce("", String::concat).trim();
				reportCreator.writeReport(report, e.getKey());
			}
		});
		
		mapReportFileJson.entrySet().forEach(e -> {
			reportCreator.createFileInfoJson(e.getValue(), e.getKey());
		});
		
		
		
		/*
		reportCreator.createReportFileInfos(failedReadFilesList,
				props.getProperty("failed.read.files") != null ? props.getProperty("failed.read.files") : FAILED_READ_FILES);
		reportCreator.createReportFileInfos(uncompressedFiles, 
				props.getProperty("uncompressed.files.file") != null ? props.getProperty("uncompressed.files.file") : UNCOMPRESSED_FILES);
		reportCreator.createReportFileInfos(failedCompressedFiles, 
				props.getProperty("failed.compressed.files.file") != null ? props.getProperty("failed.compressed.files.file") : FAILED_COMPRESSED_FILES);
		reportCreator.createReportStrings(failedKeys, 
				props.getProperty("failed.keys.file") != null ? props.getProperty("failed.keys.file") : FAILED_KEYS);
		reportCreator.createReportStrings(incompleteKeys,
				props.getProperty("incomplete.keys.file") != null ? props.getProperty("incomplete.keys.file") : INCOMPLETE_KEYS);
		reportCreator.createReportStrings(wasteKeys, 
				props.getProperty("waste.key.files") != null ? props.getProperty("waste.key.files") : WASTE_KEYS);
		reportCreator.createFileInfoJson(failedReadFilesList,
				props.getProperty("failed.read.files.json") != null ? props.getProperty("failed.read.files.json") : FAILED_READ_FILES_JSON);
		reportCreator.createFileInfoJson(uncompressedFiles, 
				props.getProperty("uncompressed.files.json") != null ? props.getProperty("uncompressed.files.json") : UNCOMPRESSED_FILES_JSON);
		reportCreator.createFileInfoJson(failedCompressedFiles, 
				props.getProperty("failed.compressed.files.json") != null ? props.getProperty("failed.compressed.files.json") : FAILED_COMPRESSED_JSON);
				*/
	}
}
