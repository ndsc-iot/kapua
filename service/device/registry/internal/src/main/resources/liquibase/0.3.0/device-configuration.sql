-- *******************************************************************************
-- Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
--
-- All rights reserved. This program and the accompanying materials
-- are made available under the terms of the Eclipse Public License v1.0
-- which accompanies this distribution, and is available at
-- http://www.eclipse.org/legal/epl-v10.html
--
-- Contributors:
--     Eurotech - initial API and implementation
-- *******************************************************************************

-- liquibase formatted sql

-- changeset device:1

-- WARNING: to be kept in sync with kapua/commons/src/main/resources/liquibase/configuration.sql

UPDATE sys_configuration SET configurations = 
	CONCAT('#', CURRENT_TIMESTAMP(), CHAR(13), CHAR(10),
        'maxNumberChildEntities=0', CHAR(13), CHAR(10),
        'infiniteChildEntities=true', CHAR(13), CHAR(10),
        'deviceUserCouplingEnabled=true', CHAR(13), CHAR(10),
        'deviceUserCouplingDefaultMode=LOOSE')
WHERE 
  SCOPE_ID = 1 AND PID = 'org.eclipse.kapua.service.device.registry.DeviceRegistryService';
