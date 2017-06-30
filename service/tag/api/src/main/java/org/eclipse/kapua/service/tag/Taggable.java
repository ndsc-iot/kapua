/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.tag;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.id.KapuaIdAdapter;

/**
 * Interface used to mark tag-able entities.
 *
 */
public interface Taggable {

    /**
     * Sets the set of {@link Tag} id of this entity.
     * 
     * @param tagIds
     *            The set {@link Tag} id to assign.
     * @since 1.0.0
     */
    public void setTagIds(Set<KapuaId> tagIds);

    /**
     * Gets the set of {@link Tag} id assigned to this entity.
     * 
     * @return The set {@link Tag} id assigned to this entity.
     * @since 1.0.0
     */
    @XmlElement(name = "tagId")
    @XmlJavaTypeAdapter(KapuaIdAdapter.class)
    public <I extends KapuaId> Set<I> getTagIds();

}
