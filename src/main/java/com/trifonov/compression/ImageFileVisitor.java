package com.trifonov.compression;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import lombok.NonNull;

@Data
public class ImageFileVisitor implements FileVisitor<Path> {

	private final Logger logger = LogManager.getLogger();

	/**
	 * Список доступных файлов для сжатия.
	 */
	// @NonNull
	private List<FileInfo> targetList;

	/**
	 * Список файлов, которые не удалось прочитать при проходе по директории.
	 */
	// @NonNull
	private List<FileInfo> failedReadFiles;

	private String sourcePathName;

	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		targetList.add(new FileInfo(file.toString(), Files.size(file)));
		//добавить обработку файла с json-объектами FileInfo (для данного файла обозначить название). если этот файл есть, то загрузить данные в targetList из него
		return FileVisitResult.CONTINUE;
	}

	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		failedReadFiles.add(new FileInfo(file.toString(), 0));
		return FileVisitResult.CONTINUE;
	}

	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	/**
	 * Метод для наполнения списка json-объектов файлов для сжатия и списка
	 * json-объектов файлов, к которым не удалось получить доступ.
	 * 
	 * @param sourcePath          Директория с файлами либо файл со списком
	 *                            json-объектов.
	 * @param initFilesList       Список json-объектов файлов для сжатия.
	 * @param failedReadFilesList Список json-объектов файлов, к которым не удалось
	 *                            получить доступ.
	 */
	private void fillFilesList() {
		Path currentPathAbs = Paths.get("").toAbsolutePath();
		logger.info("currentPathAbs = {}", currentPathAbs);
		targetList = new LinkedList<>();
		failedReadFiles = new LinkedList<>();
		try {
			Path sourcePath = Paths.get(sourcePathName);
			Files.walkFileTree(sourcePath, this);
		} catch (IOException e) {
			logger.error("Failed get list all files for compressing. IOException. ", e);
		} catch (Exception e) {
			logger.error("Failed get list all files for compressing. IOException. ", e);
		}

		/*
		 * if (imageFileVisitor.getTargetList().isEmpty()) {
		 * logger.info("File list for compressing is empty"); }
		 */
	}

}
