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

ALTER TABLE dvc_device ALTER COLUMN preferred_user_id RENAME TO last_user_id;
ALTER TABLE dvc_device ALTER COLUMN credentials_mode RENAME TO device_user_coupling_mode;
ALTER TABLE dvc_device ADD reserved_user_id BIGINT(21) DEFAULT NULL UNIQUE;

CREATE INDEX idx_device_last_user_id ON dvc_device (scope_id, last_user_id);
CREATE INDEX idx_device_reserved_user_id ON dvc_device (scope_id, reserved_user_id);
DROP INDEX idx_device_preferred_user_id;
