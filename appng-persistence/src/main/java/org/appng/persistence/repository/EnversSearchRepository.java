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
package org.appng.persistence.repository;

import java.io.Serializable;

import org.springframework.data.envers.repository.support.EnversRevisionRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 
 * A {@link SearchRepository} that additionally offers Spring Data Envers functionality
 * 
 * @author Claus Stuemke
 * 
 * @param <T>
 *            the domain class
 * @param <ID>
 *            the type of the Id of the domain class
 * @param <N>
 *            the type of the revision
 * 
 * @see SearchRepository
 * @see EnversRevisionRepository
 */

@NoRepositoryBean
public interface EnversSearchRepository<T, ID extends Serializable, N extends Number & Comparable<N>> extends
		EnversRevisionRepository<T, ID, N>, SearchRepository<T, ID> {

}
