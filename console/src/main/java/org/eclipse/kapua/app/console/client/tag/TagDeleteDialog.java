/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.client.tag;

import org.eclipse.kapua.app.console.client.messages.ConsoleTagMessages;
import org.eclipse.kapua.app.console.client.ui.dialog.entity.EntityDeleteDialog;
import org.eclipse.kapua.app.console.client.util.DialogUtils;
import org.eclipse.kapua.app.console.client.util.FailureHandler;
import org.eclipse.kapua.app.console.shared.model.GwtTag;
import org.eclipse.kapua.app.console.shared.service.GwtTagService;
import org.eclipse.kapua.app.console.shared.service.GwtTagServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class TagDeleteDialog extends EntityDeleteDialog {

    private static final GwtTagServiceAsync GWT_TAG_SERVICE = GWT.create(GwtTagService.class);
    private GwtTag gwtTag;
    private final static ConsoleTagMessages MSGS = GWT.create(ConsoleTagMessages.class);

    public TagDeleteDialog(GwtTag gwtTag) {
        this.gwtTag = gwtTag;
        DialogUtils.resizeDialog(this, 300, 135);
    }

    @Override
    public void submit() {
        GWT_TAG_SERVICE.delete(gwtTag.getScopeId(), gwtTag.getId(), new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable arg0) {
                FailureHandler.handle(arg0);
                exitStatus = false;
                exitMessage = MSGS.dialogDeleteError(arg0.getLocalizedMessage());
                hide();

            }

            @Override
            public void onSuccess(Void arg0) {
                exitStatus = true;
                exitMessage = MSGS.dialogDeleteConfirmation();
                hide();
            }
        });

    }

    @Override
    public String getHeaderMessage() {
        return MSGS.dialogDeleteHeader(gwtTag.getTagName());
    }

    @Override
    public String getInfoMessage() {
        return MSGS.dialogDeleteInfo();
    }

}
