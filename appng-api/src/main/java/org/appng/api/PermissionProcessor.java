/*
 * Copyright 2011-2017 the original author or authors.
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
package org.appng.api;

import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldPermissionType;
import org.appng.xml.platform.FieldPermissions;
import org.appng.xml.platform.Permission;

/**
 * Checks the {@link Permission}s owned by a {@link PermissionOwner}.
 * 
 * @author Matthias Müller
 * 
 */
public interface PermissionProcessor {

	/**
	 * Checks whether all of the {@link PermissionOwner}s {@link Permission}s are present.
	 * 
	 * @param permissionOwner
	 *            the {@link PermissionOwner}
	 * @return {@code true} if all permissions are present, {@code false} otherwise
	 */
	boolean hasPermissions(PermissionOwner permissionOwner);

	/**
	 * Checks whether the {@link Permission} identified by the given {@code reference} is present.
	 * 
	 * @param reference
	 *            the name of the {@link Permission} to check
	 * @return {@code true} if the permission is present, {@code false} otherwise
	 * @see Permission#getRef()
	 */
	boolean hasPermission(String reference);

	/**
	 * Checks whether the {@link Permission}s to write the value for the given {@link FieldDef} are present.
	 * 
	 * @param fieldDefinition
	 *            the {@link FieldDef} to check the write-permission for
	 * @return {@code true} if the permissions are present and the {@link FieldDef} isn't readonly, {@code false}
	 *         otherwise
	 * 
	 * @see FieldDef#getReadonly()
	 * @see FieldPermissions
	 * @see FieldPermissionType#WRITE
	 */
	boolean hasWritePermission(FieldDef fieldDefinition);

	/**
	 * Checks whether the {@link Permission}s to read the value for the given {@link FieldDef} are present.
	 * 
	 * @param fieldDefinition
	 *            the {@link FieldDef} to check the read-permission for
	 * @return {@code true} if the permissions are present, {@code false} otherwise
	 * 
	 * @see FieldPermissions
	 * @see FieldPermissionType#READ
	 */
	boolean hasReadPermission(FieldDef fieldDefinition);

}
