/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.search.indexer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.tika.parser.ParsingReader;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.search.Document;
import org.appng.api.search.DocumentEvent;
import org.appng.api.search.DocumentProducer;
import org.appng.api.search.Producer;
import org.appng.search.DocumentProvider;
import org.appng.search.Search;
import org.appng.search.indexer.IndexConfig.ConfigEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemProvider implements DocumentProvider {

	private static Logger log = LoggerFactory.getLogger(FileSystemProvider.class);
	private IndexConfig config;
	private List<String> extensions;
	private long timeout;
	private String jspExtension;
	private File dataDir;
	private Collection<? extends File> protectedFolders;
	/* if this field is present and has value 'false', the whole page is skipped while indexing */
	private static final String INDEX_PAGE = "indexPage";

	public FileSystemProvider(IndexConfig config, List<String> extensions, long timeout, String jspExtension,
			File dataDir, Collection<? extends File> protectedFolders) {
		this.config = config;
		this.extensions = extensions;
		this.timeout = timeout;
		this.jspExtension = jspExtension;
		this.dataDir = dataDir;
		this.protectedFolders = protectedFolders;

	}

	public Iterable<DocumentProducer> getDocumentProducers(Site site, Application application)
			throws InterruptedException, TimeoutException {
		return indexDirectory(site.getProperties().getInteger(SiteProperties.INDEX_FILE_SYSTEM_QUEUE_SIZE));
	}

	/**
	 * Performs the actual indexing.
	 * 
	 * @param documentQueueSize
	 *            the initial queue size for each {@link DocumentProducer}
	 * @return an {@link Iterable} of {@link DocumentProducer}s
	 * @throws InterruptedException
	 *             if such an exception occurs while adding a {@link DocumentEvent} to the indexer
	 * @throws TimeoutException
	 *             if such an exception occurs while adding a {@link DocumentEvent} to the indexer
	 */
	public Iterable<DocumentProducer> indexDirectory(int documentQueueSize)
			throws InterruptedException, TimeoutException {
		List<DocumentProducer> producers = new ArrayList<DocumentProducer>();
		log.info("dataDir: " + dataDir.getPath());

		DocumentProducer clearer = new DocumentProducer(1, Search.getDefaultAnalyzerClass(), "clear index");
		clearer.put(new DocumentEvent(null, DocumentIndexer.CLEAR_INDEX), timeout);

		producers.add(clearer);

		int numIndexed = 0;
		long start = System.currentTimeMillis();
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			log.error(dataDir + " does not exist or is not a directory");
		} else {

			Set<String> folders = config.getFolders();
			for (String folder : folders) {
				List<File> skippedFolders = new ArrayList<File>(protectedFolders);
				List<String> skipList = new ArrayList<String>(folders);
				skipList.remove(folder);
				for (String skipfolder : skipList) {
					skippedFolders.add(new File(dataDir, skipfolder));
				}
				ConfigEntry entry = config.getEntry(folder);
				File contentFolder = new File(dataDir, folder);

				DocumentProducer documentProducer = new DocumentProducer(documentQueueSize,
						entry.getAnalyzer().getClass(), "index " + contentFolder.getAbsolutePath());

				String language = entry.getLanguage();
				numIndexed += indexDirectory(documentProducer, language, contentFolder, dataDir, extensions,
						skippedFolders);
				producers.add(documentProducer);

			}
		}
		long end = System.currentTimeMillis();
		log.info("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");
		return producers;
	}

	private int indexDirectory(Producer<DocumentEvent> producer, String language, File currentDirectory, File dataDir,
			final List<String> fileTypes, List<File> protectedFolders) throws InterruptedException, TimeoutException {
		int count = 0;
		File[] fileArr = currentDirectory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				boolean accept = pathname.isDirectory()
						|| FilenameUtils.isExtension(pathname.getAbsolutePath(), fileTypes);
				return accept;
			}
		});
		if (null != fileArr) {
			int pathOffset = dataDir.getAbsolutePath().length();
			for (int i = 0; i < fileArr.length; i++) {
				File file = fileArr[i];
				if (file.isDirectory()) {
					if (!protectedFolders.contains(file)) {
						count += indexDirectory(producer, language, file, dataDir, fileTypes, protectedFolders);
					}
				} else {
					String filePath = file.getAbsolutePath().substring(pathOffset);
					filePath = filePath.replace("\\", "/");
					DocumentEvent documentEvent = indexFile(i + 1, fileArr.length, language, file, filePath);
					if (null != documentEvent) {
						producer.putWithTimeout(documentEvent, timeout);
						count++;
					}
				}
			}
		}
		return count;
	}

	DocumentEvent indexFile(int fileNo, int total, String language, File file, String serverPath)
			throws InterruptedException, TimeoutException {
		long start = System.currentTimeMillis();
		SimpleDocument document = new SimpleDocument();
		document.setPath(serverPath);
		String extension = FilenameUtils.getExtension(file.getName());
		document.setType(extension);
		document.setDate(new Date(file.lastModified()));
		document.setLanguage(language);
		String content = null;
		try {
			if (jspExtension.equals(extension)) {
				Map<String, StringBuilder> fieldMap = new ParseTags(config.getTagPrefix()).parse(file);

				StringBuilder indexPage = fieldMap.remove(INDEX_PAGE);
				if (indexPage == null || !"false".equalsIgnoreCase(indexPage.toString())) {
					StringBuilder field = fieldMap.remove(Document.FIELD_CONTENT);
					if (null != field) {
						content = field.toString();
					}
					field = fieldMap.remove(Document.FIELD_TITLE);
					if (null != field) {
						document.setName(field.toString());
					}

					for (String name : fieldMap.keySet()) {
						TextField customField = new TextField(name, fieldMap.get(name).toString(), Store.YES);
						document.addField(customField);
					}

					log.debug("indexing (" + fileNo + "/" + total + "):" + file.getAbsolutePath());
				} else {
					log.debug("skipping " + file.getAbsolutePath());
				}
			} else {
				try (Reader parsingReader = new ParsingReader(file)) {
					log.debug("indexing (" + fileNo + "/" + total + "):" + file.getAbsolutePath());
					byte[] bytes = IOUtils.toByteArray(parsingReader, Charset.defaultCharset());
					content = new String(bytes);
					document.setName(FilenameUtils.getName(file.getName()));
				}
			}
		} catch (IOException e) {
			log.error("error while indexing " + file.getAbsolutePath(), e);
			return null;
		}
		document.setContent(content);
		document.setId(serverPath);
		long duration = System.currentTimeMillis() - start;
		log.trace("extraction took " + (duration) + "ms");
		return new DocumentEvent(document, Document.CREATE);
	}

}
