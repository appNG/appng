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
package org.appng.core.controller.filter.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.model.Properties;
import org.appng.api.support.SiteAwareObjectInputStream;
import org.appng.api.support.environment.DefaultEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.session.data.mongo.AbstractMongoSessionConverter;
import org.springframework.session.data.mongo.MongoExpiringSession;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * A {@link ServletContextListener} responsible for preparing the {@link MongoOperationsSessionRepository} to be used by
 * {@link SessionFilterDelegate}. Uses a custom {@link AbstractMongoSessionConverter} to do so.
 * 
 * <p>
 * Add the following to {@code web.xml}
 * 
 * <pre>
 * &lt;listener>
 *     &lt;listener-class>org.appng.core.controller.filter.session.MongoSessionInitializer&lt;/listener-class>
 * &lt;/listener>
 * </pre>
 * 
 * @author Matthias Müller
 *
 */
public class MongoSessionInitializer implements ServletContextListener {

	private static final Logger log = LoggerFactory.getLogger(MongoSessionInitializer.class);

	public void contextInitialized(ServletContextEvent sce) {
		Environment environment = DefaultEnvironment.get(sce.getServletContext());
		ApplicationContext ctx = environment.getAttribute(Scope.PLATFORM, Platform.Environment.CORE_PLATFORM_CONTEXT);
		MongoOperationsSessionRepository repository = ctx.getBean(MongoOperationsSessionRepository.class);
		Properties platformConfig = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
		Integer sessionTimeout = platformConfig.getInteger(Platform.Property.SESSION_TIMEOUT);
		repository.setMaxInactiveIntervalInSeconds(sessionTimeout);
		repository.setMongoSessionConverter(new MongoSessionConverter(environment));
		ctx.getBean(SessionRepositoryFilter.class).setServletContext(sce.getServletContext());
	}

	public void contextDestroyed(ServletContextEvent sce) {

	}

	class MongoSessionConverter extends AbstractMongoSessionConverter {

		private static final String _ID = "_id";
		private static final String _CREATION_TIME = "_creation_time";
		private static final String _LAST_ACCESS_TIME = "_last_access_time";
		private static final String _MAX_INACTIVE = "_max_inactive";
		private static final String _DATA = "_data";
		private final Environment environment;

		MongoSessionConverter(Environment environment) {
			this.environment = environment;
		}

		protected DBObject convert(MongoExpiringSession session) {
			long start = System.currentTimeMillis();
			BasicDBObject mongoSession = new BasicDBObject();
			try (
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(bos)) {

				SessionUtils.writeToStream(oos, session);

				mongoSession.put(_ID, session.getId());
				mongoSession.put(_CREATION_TIME, session.getCreationTime());
				mongoSession.put(_LAST_ACCESS_TIME, session.getLastAccessedTime());
				mongoSession.put(_MAX_INACTIVE, session.getMaxInactiveIntervalInSeconds());
				mongoSession.put(_DATA, bos.toByteArray());
				log.trace("session -> mongo: {}", System.currentTimeMillis() - start);
			} catch (IOException e) {
				log.error("error writing session", e);
			}
			return mongoSession;
		}

		protected MongoExpiringSession convert(DBObject mongoSession) {
			long start = System.currentTimeMillis();
			String id = (String) mongoSession.get(_ID);
			Long creationTime = (Long) mongoSession.get(_CREATION_TIME);
			Long lastAccessTime = (Long) mongoSession.get(_LAST_ACCESS_TIME);
			Integer maxInactive = (Integer) mongoSession.get(_MAX_INACTIVE);

			MongoExpiringSession session = new MongoExpiringSession(id, maxInactive);
			session.setCreationTime(creationTime);
			session.setLastAccessedTime(lastAccessTime);

			byte[] data = (byte[]) mongoSession.get(_DATA);
			if (data != null) {
				try (
						ByteArrayInputStream is = new ByteArrayInputStream(data);
						ObjectInputStream ois = new SiteAwareObjectInputStream(is, environment)) {

					SessionUtils.readFromStream(ois, session);
				} catch (IOException | ClassNotFoundException e) {
					log.error("error reading session", e);
				}
			}
			log.trace("mongo -> session: {}", System.currentTimeMillis() - start);
			return session;
		}

		protected Query getQueryForIndex(String indexName, Object indexValue) {
			return null;
		}

	}
}
