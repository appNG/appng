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
package org.appng.upngizr.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * The controller performing the update operation.
 * 
 * @author Matthias Müller
 *
 */
@Slf4j
@RestController
public class Updater {

	private static final String BUILD = "{build}";
	private static final String VERSION = "{version}";
	private static final String APPNG_APPLICATION = String.format("appng-application-%s.war", VERSION);
	private static final String APPNGIZER_APPLICATION = String.format("appng-appngizer-%s.war", VERSION);
	private static final String INIT_PARAM_BLOCK_REMOTE_IPS = "blockRemoteIPs";
	private static final String INIT_PARAM_BUILD_REPOSITORY = "buildRepository";
	private static final String INIT_PARAM_REPLACE_BIN = "replaceBin";
	private static final String INIT_PARAM_REPLACE_PLATFORMCONTEXT = "replacePlatformContext";
	private static final String INIT_PARAM_REPLACE_WEB_XML = "replaceWebXml";
	private static final String WEB_INF = "WEB-INF/";
	private static final String WEB_INF_CLASSES = WEB_INF + "classes/";
	private static final String WEB_INF_LIB = WEB_INF + "lib/";
	private ServletContext context;
	private String buildRepository = String.format("https://appng.org/appng/builds/%s/", BUILD);
	private boolean replacePlatformContext = true;
	private boolean replaceWebXml = true;
	private boolean replaceBin = false;
	private boolean blockRemoteIps = true;
	private List<String> localAdresses = new ArrayList<>();
	private AtomicBoolean isUpdateRunning = new AtomicBoolean(false);
	private AtomicReference<Double> completed = new AtomicReference<Double>(0.0d);
	private AtomicReference<String> status = new AtomicReference<String>("Starting update");

	@Autowired
	public Updater(ServletContext context) {
		this.context = context;
		if (null != context.getInitParameter(INIT_PARAM_BUILD_REPOSITORY)) {
			this.buildRepository = context.getInitParameter(INIT_PARAM_BUILD_REPOSITORY);
		}
		if (null != context.getInitParameter(INIT_PARAM_REPLACE_PLATFORMCONTEXT)) {
			this.replacePlatformContext = Boolean.valueOf(context.getInitParameter(INIT_PARAM_REPLACE_PLATFORMCONTEXT));
		}
		if (null != context.getInitParameter(INIT_PARAM_REPLACE_WEB_XML)) {
			this.replaceWebXml = Boolean.valueOf(context.getInitParameter(INIT_PARAM_REPLACE_WEB_XML));
		}
		if (null != context.getInitParameter(INIT_PARAM_REPLACE_BIN)) {
			this.replaceWebXml = Boolean.valueOf(context.getInitParameter(INIT_PARAM_REPLACE_BIN));
		}
		if (null != context.getInitParameter(INIT_PARAM_BLOCK_REMOTE_IPS)) {
			this.blockRemoteIps = Boolean.valueOf(context.getInitParameter(INIT_PARAM_BLOCK_REMOTE_IPS));
		}
		if (blockRemoteIps) {
			try {
				Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
				while (networkInterfaces.hasMoreElements()) {
					NetworkInterface networkInterface = networkInterfaces.nextElement();
					Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
					while (inetAddresses.hasMoreElements()) {
						String hostAddress = inetAddresses.nextElement().getHostAddress();
						int idx = hostAddress.indexOf('%');
						localAdresses.add(hostAddress.substring(0, idx > 0 ? idx : hostAddress.length()));
					}
				}
				log.info("Allowed local addresses: {}", StringUtils.collectionToCommaDelimitedString(localAdresses));
			} catch (SocketException e) {
				log.error("error retrieving networkinterfaces", e);
			}
		}
	}

	@RequestMapping(method = RequestMethod.GET, path = "/update/start/{version:.+}", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> getStartPage(@PathVariable("version") String version,
			@RequestParam(required = false, defaultValue = "") String onSuccess, HttpServletRequest request)
			throws IOException, URISyntaxException {
		if (isBlocked(request) || isUpdateRunning.get()) {
			return forbidden();
		}

		Resource artifactResource = getArtifact(version, APPNG_APPLICATION);
		if (!artifactResource.exists()) {
			return notFound(artifactResource);
		}

		ClassPathResource resource = new ClassPathResource("updater.html");
		String content = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
		int serverPort = request.getServerPort();
		String uppNGizrBase = String.format(serverPort == 80 ? "//%s/upNGizr" : "//%s:%s/upNGizr",
				request.getServerName(), serverPort);
		content = content.replace("<target>", onSuccess).replace("<path>", uppNGizrBase);
		content = content.replace("<version>", version).replace("<button>", "Update to " + version);
		return new ResponseEntity<>(content, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/update/status", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Status> getStatus() {
		Status status = new Status(this.status.get(), completed.get());
		return new ResponseEntity<Updater.Status>(status, HttpStatus.OK);
	}

	@RequestMapping(path = "/checkVersionAvailable/{version:.+}", method = RequestMethod.GET)
	public ResponseEntity<Void> checkVersionAvailable(@PathVariable("version") String version,
			HttpServletRequest request) throws IOException {
		if (isBlocked(request)) {
			return forbidden();
		}
		Resource resource = getArtifact(version, APPNG_APPLICATION);
		if (!resource.exists()) {
			return notFound(resource);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(path = "/update/{version:.+}", produces = MediaType.TEXT_PLAIN_VALUE, method = RequestMethod.POST)
	public ResponseEntity<String> updateAppng(@PathVariable("version") String version,
			@RequestParam(required = false) String onSuccess, HttpServletRequest request) {
		if (isBlocked(request) || isUpdateRunning.get()) {
			return forbidden();
		}

		isUpdateRunning.set(true);
		try {
			Resource appNGArchive = getArtifact(version, APPNG_APPLICATION);
			if (!appNGArchive.exists()) {
				return notFound(appNGArchive);
			}
			status.set("Stopping appNG");
			completed.set(5.0d);
			getHost().setAutoDeploy(false);
			Container appNGizerContext = stopContext(getAppNGizerContext());
			Container appNGContext = stopContext(getAppNGContext());
			completed.set(30.0d);
			updateAppNG(appNGArchive, UpNGizr.appNGHome);
			status.set("Starting appNG");
			completed.set(81.0d);
			log.info(status.get());

			ExecutorService contextStarter = Executors.newFixedThreadPool(1, new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("upNGizr updater");
					t.setPriority(Thread.MAX_PRIORITY);
					return t;
				}
			});

			Future<Void> startAppNG = contextStarter.submit(() -> {
				startContext(appNGContext);
				return null;
			});
			Long duration = waitFor(startAppNG, 95.0d, 3);
			log.info("Started appNG in {} seconds.", duration / 1000);

			if (null != appNGizerContext) {

				Future<Void> startAppNGizer = contextStarter.submit(() -> {
					updateAppNGizer(getArtifact(version, APPNGIZER_APPLICATION), UpNGizr.appNGizerHome);
					status.set("Starting appNGizer");
					log.info(status.get());
					startContext(appNGizerContext);
					completed.set(98.0d);
					return null;
				});
				duration = waitFor(startAppNGizer, 98.0d, 2);
				log.info("Started appNGizer in {} seconds.", duration / 1000);
			}
			contextStarter.shutdown();

			completed.set(100.0d);
			String statusLink = StringUtils.isEmpty(onSuccess) ? ""
					: (String.format("<br/>Forwarding to<br/><a href=\"%s\">%s</a>", onSuccess, onSuccess));
			status.set("Update complete." + statusLink);
			return new ResponseEntity<>("OK", HttpStatus.OK);
		} catch (Exception e) {
			log.error("error", e);
		} finally {
			isUpdateRunning.set(false);
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Long waitFor(Future<Void> task, double startValue, int sleepSecond)
			throws InterruptedException, ExecutionException {
		long start = System.currentTimeMillis();
		while (!task.isDone()) {
			Thread.sleep(sleepSecond * 1000);
			if (completed.get().compareTo(startValue) == -1) {
				completed.set(completed.get() + 1.0d);
			}
		}
		return System.currentTimeMillis() - start;
	}

	private <T> ResponseEntity<T> forbidden() {
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	private <T> ResponseEntity<T> notFound(Resource resource) throws IOException {
		log.warn("{} does not exist!", resource.getURL());
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	private boolean isBlocked(HttpServletRequest request) {
		log.info("Source: {}", request.getRemoteAddr());
		boolean isBlocked = blockRemoteIps && !localAdresses.isEmpty()
				&& !localAdresses.contains(request.getRemoteAddr());
		if (isBlocked) {
			log.info("remote address {} is not in list of allowed addresses ({})", request.getRemoteAddr(),
					StringUtils.collectionToDelimitedString(localAdresses, " "));
		}
		return isBlocked;
	}

	private Resource getArtifact(String version, String filename) throws MalformedURLException {
		CharSequence build = version.endsWith("-SNAPSHOT") ? "snapshot" : "stable";
		String url = buildRepository.replace(BUILD, build).replace(VERSION, version)
				+ filename.replace(VERSION, version);
		return new UrlResource(url);
	}

	private Container stopContext(Container context) {
		if (null != context) {
			try {
				context.stop();
				return context;
			} catch (LifecycleException e) {
				log.error("error stopping context", e);
			}
		}
		return null;
	}

	private void startContext(Container context) {
		if (null != context) {
			try {
				context.start();
			} catch (LifecycleException e) {
				log.error("error starting context", e);
			}
		}
	}

	protected void updateAppNG(Resource resource, String appNGHome)
			throws IOException, ZipException, FileNotFoundException {
		status.set(String.format("Downloading update %s", resource.getFilename()));

		long contentLength = resource.contentLength();
		long sizeMB = contentLength / 1024 / 1024;
		log.info("reading {} MB from {}", sizeMB, resource.getDescription());

		long start = System.currentTimeMillis();
		InputStream is = resource.getInputStream();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] bytes = new byte[8192];
		int count = -1;
		long read = 0;
		int progress = 0;
		while ((count = is.read(bytes, 0, bytes.length)) != -1) {
			os.write(bytes, 0, count);
			read += count;
			int currentProgress = ((int) (((double) read / (double) contentLength) * 100));
			if (progress != currentProgress && currentProgress % 5 == 0) {
				long readMB = read / 1024 / 1024;
				log.info("retrieved {}/{} MB ({}%)", readMB, sizeMB, currentProgress);
				completed.set(30.0d + currentProgress / 2.5d);
				status.set(String.format("Downloaded " + readMB + " of " + sizeMB + "MB"));
			}
			progress = currentProgress;
		}
		System.err.println("");
		long duration = (System.currentTimeMillis() - start) / 1000;
		log.debug("downloading {} MB took {}s ({}MB/s)", sizeMB, duration, sizeMB / (duration == 0 ? 1 : duration));

		byte[] data = os.toByteArray();

		Path warArchive = Files.createTempFile(null, null);
		IOUtils.write(data, new FileOutputStream(warArchive.toFile()));

		File libFolder = new File(appNGHome, WEB_INF_LIB);
		if (libFolder.exists()) {
			FileUtils.cleanDirectory(libFolder);
			log.info("cleaning {}", libFolder);
		}
		completed.set(75.0d);
		status.set("Extracting files");
		ZipFile zip = new ZipFile(warArchive.toFile());
		Enumeration<? extends ZipEntry> entries = zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			String name = entry.getName();
			String folder = name.substring(0, name.lastIndexOf('/') + 1);
			if (!entry.isDirectory()) {
				switch (folder) {
				case WEB_INF:
					if (replaceWebXml) {
						writeFile(appNGHome, zip.getInputStream(entry), name);
					}
					break;
				case WEB_INF_LIB:
				case WEB_INF_CLASSES:
					writeFile(appNGHome, zip.getInputStream(entry), name);
					break;
				case WEB_INF + "conf/":
					if (replacePlatformContext && name.endsWith("platformContext.xml")) {
						writeFile(appNGHome, zip.getInputStream(entry), name);
					}
					break;
				case WEB_INF + "/bin/":
					if (replaceBin) {
						writeFile(appNGHome, zip.getInputStream(entry), name);
					}
					break;
				default:
					log.info("Skipping {}", name);
					break;
				}
			}

		}
		zip.close();
		warArchive.toFile().delete();
		completed.set(80.0d);
	}

	protected void updateAppNGizer(Resource resource, String appNGizerHome) throws RestClientException, IOException {
		if (!(resource.exists() && new File(appNGizerHome).exists())) {
			return;
		}
		Path warArchive = Files.createTempFile(null, null);
		try (
				FileOutputStream out = new FileOutputStream(warArchive.toFile());
				InputStream is = resource.getInputStream()) {
			IOUtils.copy(is, out);
			try (ZipFile zip = new ZipFile(warArchive.toFile())) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					String folder = name.substring(0, name.lastIndexOf('/') + 1);
					if (!entry.isDirectory()) {
						if (folder.startsWith(WEB_INF_CLASSES)) {
							writeFile(appNGizerHome, zip.getInputStream(entry), name);
						} else {
							switch (folder) {
							case WEB_INF:
								if (!(WEB_INF + "web.xml").equals(name)) {
									writeFile(appNGizerHome, zip.getInputStream(entry), name);
								}
								break;
							case WEB_INF_LIB:
								writeFile(appNGizerHome, zip.getInputStream(entry), name);
								break;
							default:
								log.info("Skipping {}", name);
								break;
							}
						}
					}
				}
			}
		} finally {
			warArchive.toFile().delete();
		}
	}

	private void writeFile(String parentFolder, InputStream is, String name) throws IOException, FileNotFoundException {
		byte[] data = IOUtils.toByteArray(is);
		File targetFile = new File(parentFolder, name);
		String normalizedName = FilenameUtils.normalize(targetFile.getAbsolutePath());
		targetFile.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(normalizedName);
		IOUtils.write(data, fos);
		fos.close();
		log.info("wrote {}", normalizedName);
	}

	protected Container getAppNGContext() {
		return getHost().findChild("");
	}

	protected Container getAppNGizerContext() {
		return getHost().findChild("/" + UpNGizr.APPNGIZER);
	}

	private Host getHost() {
		return (Host) context.getAttribute(UpNGizr.HOST);
	}

	@Data
	class Status {
		private final double completed;
		private final String taskName;
		private boolean done = false;

		Status(String taskName, double completed) {
			this.taskName = taskName;
			this.completed = completed;
			done = completed >= 100.00d;
		}

	}
}
