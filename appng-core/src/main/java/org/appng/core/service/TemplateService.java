/*
 * Copyright 2011-2020 the original author or authors.
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
package org.appng.core.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.appng.api.BusinessException;
import org.appng.api.Platform;
import org.appng.api.SiteProperties;
import org.appng.api.model.Identifier;
import org.appng.api.model.Properties;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.core.domain.Template;
import org.appng.core.domain.TemplateResource;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.RepositoryUtils;
import org.appng.core.model.ZipFileProcessor;
import org.appng.core.repository.TemplateRepository;
import org.appng.core.repository.TemplateResourceRepository;
import org.appng.xml.MarshallService;
import org.appng.xml.application.TemplateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * A service offering methods to deal with templates.
 * 
 * @author Matthias Müller
 */
@Slf4j
@Transactional
public class TemplateService {

	private static final String TEMPLATE_XML = "template.xml";
	private static final String XSL = "xsl";
	private static final String CONF = "conf";
	private static final String PLATFORM_XSL = XSL + "/platform.xsl";
	public static final String PLATFORM_XML = CONF + "/platform.xml";

	@Autowired
	protected TemplateRepository templateRepository;
	@Autowired
	protected TemplateResourceRepository templateResourceRepository;

	/**
	 * Returns a {@link ZipFileProcessor}{@code <}{@link Template}{@code >}, which used to extract a template-zip.
	 * 
	 * @return the {@link ZipFileProcessor}
	 */
	public ZipFileProcessor<Template> getTemplateExtractor() {
		ZipFileProcessor<Template> templateExtractor = new ZipFileProcessor<Template>() {
			public Template process(ZipFile zipFile) throws IOException {
				org.appng.xml.application.Template templateXml = null;
				List<TemplateResource> resources = new ArrayList<>();
				try {
					Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
					MarshallService applicationMarshallService = MarshallService.getApplicationMarshallService();
					while (entries.hasMoreElements()) {
						ZipArchiveEntry entry = entries.nextElement();
						String path = entry.getName();
						path = path.substring(path.indexOf('/') + 1);
						if (!entry.isDirectory()) {
							try (
									InputStream in = zipFile.getInputStream(entry);
									ByteArrayOutputStream out = new ByteArrayOutputStream()) {
								try {
									IOUtils.copy(in, out);
									TemplateResource templateResource = new TemplateResource();
									templateResource.setName(path);
									templateResource.setFileVersion(entry.getLastModifiedDate());
									templateResource.setResourceType(ResourceType.RESOURCE);
									templateResource.setBytes(out.toByteArray());
									templateResource.calculateChecksum();
									resources.add(templateResource);

									if (path.equals(TEMPLATE_XML)) {
										templateXml = applicationMarshallService.unmarshall(
												new ByteArrayInputStream(templateResource.getBytes()),
												org.appng.xml.application.Template.class);
									}
								} catch (IOException ioe) {
									throw ioe;
								}
							}
							LOGGER.info("added resource {}", path);
						}
					}
				} catch (JAXBException e) {
					throw new IOException(e);
				}
				ZipFile.closeQuietly(zipFile);
				Template template = new Template(templateXml);
				for (TemplateResource templateResource : resources) {
					templateResource.setTemplate(template);
					template.getResources().add(templateResource);
				}
				return template;
			}
		};
		return templateExtractor;
	}

	/**
	 * Returns a {@link ZipFileProcessor}{@code <}{@link Template}{@code >}, used to check whether a {@link ZipFile}
	 * contains a valid template.
	 * 
	 * @param originalFilename
	 *            the original file name of the {@link ZipFile}
	 * @return the {@link ZipFileProcessor}
	 */
	public static ZipFileProcessor<org.appng.xml.application.Template> getTemplateInfo(final String originalFilename) {
		ZipFileProcessor<org.appng.xml.application.Template> zipFileProcessor = new ZipFileProcessor<org.appng.xml.application.Template>() {

			public org.appng.xml.application.Template process(ZipFile zipFile) throws IOException {
				org.appng.xml.application.Template template = null;

				try {
					Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
					String templateRoot = entries.nextElement().getName();

					String templatePath = templateRoot + TEMPLATE_XML;
					ZipArchiveEntry templateFile = zipFile.getEntry(templatePath);
					if (null == templateFile) {
						throw new FileNotFoundException(TEMPLATE_XML + " not found");
					}
					String masterPath = templateRoot + PLATFORM_XML;
					ZipArchiveEntry masterFile = zipFile.getEntry(masterPath);
					if (null == masterFile) {
						throw new FileNotFoundException(PLATFORM_XML + " not found");
					}
					try (InputStream masterIs = zipFile.getInputStream(masterFile)) {
						validateResource(MarshallService.getMarshallService(), masterPath, masterIs,
								org.appng.xml.platform.Platform.class);

						try (InputStream templateIs = zipFile.getInputStream(templateFile)) {
							template = validateResource(MarshallService.getApplicationMarshallService(), templatePath,
									templateIs, org.appng.xml.application.Template.class);

							if (TemplateType.XSL.equals(template.getType())) {
								ZipArchiveEntry xsl = zipFile.getEntry(templateRoot + PLATFORM_XSL);
								if (null == xsl) {
									throw new FileNotFoundException(PLATFORM_XSL + " not found");
								}
							}
						}
					}
				} catch (FileNotFoundException ioe) {
					LOGGER.debug("not a valid template: {} ({})", originalFilename, ioe.getMessage());
				} catch (IOException ioe) {
					LOGGER.debug("error while reading from {} ({})", originalFilename, ioe.toString());
				} catch (JAXBException e) {
					LOGGER.trace("error while unmarshalling", e);
					LOGGER.debug("not a valid template: {} ({})", originalFilename, e.toString());
				} finally {
					ZipFile.closeQuietly(zipFile);
				}
				return template;
			}
		};
		return zipFileProcessor;
	}

	@SuppressWarnings("unchecked")
	private static <T> T validateResource(MarshallService marshallService, String name, InputStream is,
			Class<T> targetType) throws JAXBException {
		Object object = marshallService.unmarshall(is);
		if (null == object || (!object.getClass().isAssignableFrom(targetType))) {
			throw new JAXBException(name + "has invalid type/ was not found");
		}
		return (T) object;
	}

	/**
	 * Returns a list of all installed templates.
	 * 
	 * @return a list containing the {@link Identifier}s of all installed templates
	 */
	public List<Identifier> getInstalledTemplates() {
		return new ArrayList<>(templateRepository.findAll());
	}

	class TemplateIdentifier implements Identifier {

		private org.appng.xml.application.Template template;

		TemplateIdentifier(org.appng.xml.application.Template template) {
			this.template = template;
		}

		public Integer getId() {
			return hashCode();
		}

		public String getPackageVersion() {
			return template.getVersion();
		}

		public boolean isInstalled() {
			return true;
		}

		public boolean isSnapshot() {
			return getPackageVersion().endsWith(RepositoryUtils.SNAPSHOT);
		}

		public String getName() {
			return template.getName();
		}

		public String getDescription() {
			return template.getDescription();
		}

		public String getDisplayName() {
			return template.getDisplayName();
		}

		public String getTimestamp() {
			return template.getTimestamp();
		}

		public String getLongDescription() {
			return template.getDescription();
		}

		public String getAppNGVersion() {
			return template.getAppngVersion();
		}

		public Date getVersion() {
			return null;
		}

	}

	/**
	 * Materializes the given template into the filesystem.
	 * 
	 * @param template
	 *            the {@link Template} to materialize
	 * @param platformConfig
	 *            the platform configuration
	 * @param siteProps
	 *            the {@link Site}'s {@link Properties}
	 */
	public static void materializeTemplate(Template template, Properties platformConfig, Properties siteProps) {
		File templateTargetDir = getTemplateRepoFolder(platformConfig, siteProps);
		deleteTemplateFolder(templateTargetDir);
		for (TemplateResource resource : template.getResources()) {
			File targetFile = new File(templateTargetDir, resource.getName());
			targetFile.getParentFile().mkdirs();
			try (
					ByteArrayInputStream in = new ByteArrayInputStream(resource.getBytes());
					FileOutputStream out = new FileOutputStream(targetFile)) {
				LOGGER.trace("writing {}", targetFile);
				IOUtils.copy(in, out);
				targetFile.setLastModified(resource.getFileVersion().getTime());
			} catch (IOException e) {
				LOGGER.warn("errror writing template resource", e);
			}
		}
	}

	/**
	 * The active template for the given {@link Site} is being copied to
	 * 
	 * <pre>
	 * {@value org.appng.api.SiteProperties#SITE_ROOT_DIR}/{@value org.appng.api.SiteProperties#WWW_DIR}/{@value org.appng.api.Platform.Property#TEMPLATE_PREFIX}
	 * </pre>
	 * 
	 * @param platformConfig
	 *            the platform configuration
	 * @param siteProps
	 *            the {@link Site}'s {@link Properties}
	 * @param templateRealPath
	 *            the root path to the platform's template directory
	 */
	public static void copyTemplate(Properties platformConfig, Properties siteProps, String templateRealPath) {
		String template = siteProps.getString(SiteProperties.TEMPLATE);
		File templateSourceDir = new File(templateRealPath, template);
		File templateTargetDir = getTemplateRepoFolder(platformConfig, siteProps);
		deleteTemplateFolder(templateTargetDir);
		try {
			FileUtils.copyDirectory(templateSourceDir, templateTargetDir, new FileFilter() {
				public boolean accept(File file) {
					String parent = file.getParentFile().getName();
					boolean isTemplateXsl = file.getName().equals(TEMPLATE_XML);
					boolean acceptFile = !(isTemplateXsl || parent.equals(CONF) || parent.equals(XSL));
					boolean acceptFolder = !(file.getName().equals(CONF) || file.getName().equals(XSL));
					return acceptFile && acceptFolder;
				}
			});
			LOGGER.info("copying template from {} to {}", templateSourceDir.getAbsolutePath(),
					templateTargetDir.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.warn(String.format("error while copying template from %s to %s", templateSourceDir.getAbsolutePath(),
					templateTargetDir.getAbsolutePath()), e);
		}
	}

	public Template installTemplate(PackageArchive packageArchive) throws BusinessException {
		try {
			ZipFileProcessor<Template> templateExtractor = getTemplateExtractor();
			Template template = packageArchive.processZipFile(templateExtractor);

			Template existing = templateRepository.findByName(template.getName());
			if (null == existing) {
				templateRepository.save(template);
			} else {
				List<TemplateResource> resources = existing.getResources();
				templateResourceRepository.delete(resources);
				resources.clear();
				existing.update(template);
				template = existing;
			}
			return template;
		} catch (IOException e) {
			throw new BusinessException("error while provisioning " + packageArchive.toString(), e);
		}
	}

	public Template getTemplateByDisplayName(String name) {
		Template template = templateRepository.findByDisplayName(name);
		if (null != template) {
			template.getResources().size();
		}
		return template;
	}

	public Template getTemplateByName(String name) {
		return templateRepository.findByName(name);
	}

	public Integer deleteTemplate(Template template) {
		if (null == template) {
			return -1;
		}
		templateRepository.delete(template);
		return 0;
	}

	public static File getTemplateRepoFolder(Properties platformConfig, Properties siteProps) {
		String templatePrefix = platformConfig.getString(Platform.Property.TEMPLATE_PREFIX);
		String siteWwwDir = siteProps.getString(SiteProperties.WWW_DIR);
		String siteRoot = siteProps.getString(SiteProperties.SITE_ROOT_DIR);
		return new File(new File(siteRoot, siteWwwDir), templatePrefix);
	}

	protected static void deleteTemplateFolder(File templateTargetDir) {
		if (templateTargetDir.exists()) {
			FileUtils.deleteQuietly(templateTargetDir);
			LOGGER.info("clearing {}", templateTargetDir.getAbsolutePath());
		}
	}

	public org.appng.xml.application.Template getTemplate(String templateDir) throws IOException, JAXBException {
		return validateResource(MarshallService.getApplicationMarshallService(), TEMPLATE_XML,
				new FileInputStream(new File(templateDir, TEMPLATE_XML)), org.appng.xml.application.Template.class);
	}

}
