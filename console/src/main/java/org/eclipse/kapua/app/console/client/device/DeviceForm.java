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
package org.eclipse.kapua.app.console.client.device;

import java.util.List;

import org.eclipse.kapua.app.console.client.messages.ConsoleMessages;
import org.eclipse.kapua.app.console.client.util.ConsoleInfo;
import org.eclipse.kapua.app.console.client.util.Constants;
import org.eclipse.kapua.app.console.client.util.DialogUtils;
import org.eclipse.kapua.app.console.client.util.FailureHandler;
import org.eclipse.kapua.app.console.client.util.KapuaSafeHtmlUtils;
import org.eclipse.kapua.app.console.client.util.TextFieldValidator;
import org.eclipse.kapua.app.console.client.util.TextFieldValidator.FieldType;
import org.eclipse.kapua.app.console.shared.model.GwtDevice;
import org.eclipse.kapua.app.console.shared.model.GwtDeviceCreator;
import org.eclipse.kapua.app.console.shared.model.GwtDeviceQueryPredicates;
import org.eclipse.kapua.app.console.shared.model.GwtGroup;
import org.eclipse.kapua.app.console.shared.model.GwtSession;
import org.eclipse.kapua.app.console.shared.model.GwtXSRFToken;
import org.eclipse.kapua.app.console.shared.model.GwtDevice.GwtDeviceUserCouplingMode;
import org.eclipse.kapua.app.console.shared.model.user.GwtUser;
import org.eclipse.kapua.app.console.shared.service.GwtDeviceService;
import org.eclipse.kapua.app.console.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kapua.app.console.shared.service.GwtGroupService;
import org.eclipse.kapua.app.console.shared.service.GwtGroupServiceAsync;
import org.eclipse.kapua.app.console.shared.service.GwtSecurityTokenService;
import org.eclipse.kapua.app.console.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kapua.app.console.shared.service.GwtUserService;
import org.eclipse.kapua.app.console.shared.service.GwtUserServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.ListLoadResult;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DeviceForm extends Window {

    private static final ConsoleMessages MSGS = GWT.create(ConsoleMessages.class);

    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);
    private final GwtUserServiceAsync gwtUserService = GWT.create(GwtUserService.class);
    private final GwtGroupServiceAsync gwtGroupService = GWT.create(GwtGroupService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);

    private FormPanel formPanel;
    private GwtDevice selectedDevice;
    private GwtSession currentSession;

    // General info fields
    private LabelField clientIdLabel;
    private TextField<String> clientIdField;
    private ComboBox<GwtGroup> groupCombo;
    private TextField<String> displayNameField;
    private SimpleComboBox<GwtDeviceQueryPredicates.GwtDeviceStatus> statusCombo;

    // Security Options fields
    private ComboBox<GwtUser> reservedUserCombo;
    private TextField<String> lastUserField;
    private SimpleComboBox<String> deviceUserCouplingMode;
    // private SimpleComboBox<String> credentialsTightCombo;
    // private ComboBox<GwtUser> deviceUserCombo;
    // private CheckBox allowCredentialsChangeCheckbox;

    // Custom attributes
    private TextField<String> customAttribute1Field;
    private TextField<String> customAttribute2Field;
    private TextField<String> customAttribute3Field;
    private TextField<String> customAttribute4Field;
    private TextField<String> customAttribute5Field;

    private NumberField optlock;
    
    private static final GwtGroup NO_GROUP;
    private static final GwtUser NO_USER;
    
    static {
        NO_GROUP = new GwtGroup();
        NO_GROUP.setGroupName(MSGS.deviceFormNoGroup());
        NO_GROUP.setId(null);
        NO_USER = new GwtUser();
        NO_USER.setUsername(MSGS.deviceFormNoUser());
        NO_USER.setId(null);
    }
    public DeviceForm(GwtSession currentSession) {
        this(null, currentSession);
    }

    public DeviceForm(GwtDevice gwtDevice, GwtSession currentSession) {
        this.selectedDevice = gwtDevice;
        this.currentSession = currentSession;

        setModal(true);
        setLayout(new FitLayout());
        setResizable(false);
        setHeading(this.selectedDevice == null ? MSGS.deviceFormHeadingNew()
                : MSGS.deviceFormHeadingEdit(this.selectedDevice.getDisplayName() != null ? this.selectedDevice.getDisplayName() : this.selectedDevice.getClientId()));

        DialogUtils.resizeDialog(this, 550, 570);
        
    }

    protected void onRender(Element parent, int index) {
        super.onRender(parent, index);

        FormData formData = new FormData("-20");

        formPanel = new FormPanel();
        formPanel.setFrame(false);
        formPanel.setBodyBorder(false);
        formPanel.setHeaderVisible(false);
        formPanel.setWidth(310);
        formPanel.setScrollMode(Scroll.AUTOY);
        formPanel.setStyleAttribute("padding-bottom", "0px");
        formPanel.setLayout(new FlowLayout());

        // Device general info fieldset
        FieldSet fieldSet = new FieldSet();
        FormLayout layout = new FormLayout();
        layout.setLabelWidth(Constants.LABEL_WIDTH_DEVICE_FORM);
        fieldSet.setLayout(layout);
        fieldSet.setHeading(MSGS.deviceFormFieldsetGeneralInfo());

        // Device Client ID
        clientIdLabel = new LabelField();
        clientIdLabel.setFieldLabel(MSGS.deviceFormClientID());
        clientIdLabel.setLabelSeparator(":");
        clientIdLabel.setWidth(225);
        fieldSet.add(clientIdLabel, formData);

        clientIdField = new TextField<String>();
        clientIdField.setAllowBlank(false);
        clientIdField.setName("clientID");
        clientIdField.setFieldLabel(MSGS.deviceFormClientID());
        clientIdField.setValidator(new TextFieldValidator(clientIdField, FieldType.DEVICE_CLIENT_ID));
        clientIdField.setWidth(225);

        fieldSet.add(clientIdField, formData);

        // Display name
        displayNameField = new TextField<String>();
        displayNameField.setAllowBlank(true);
        displayNameField.setName("displayName");
        displayNameField.setFieldLabel(MSGS.deviceFormDisplayName());
        displayNameField.setWidth(225);
        fieldSet.add(displayNameField, formData);

        // Device Status
        statusCombo = new SimpleComboBox<GwtDeviceQueryPredicates.GwtDeviceStatus>();
        statusCombo.setName("status");
        statusCombo.setFieldLabel(MSGS.deviceFormStatus());
        statusCombo.setEditable(false);
        statusCombo.setTriggerAction(TriggerAction.ALL);

        statusCombo.setEmptyText(MSGS.deviceFilteringPanelStatusEmptyText());
        statusCombo.add(GwtDeviceQueryPredicates.GwtDeviceStatus.ENABLED);
        statusCombo.add(GwtDeviceQueryPredicates.GwtDeviceStatus.DISABLED);

        fieldSet.add(statusCombo, formData);

        groupCombo = new ComboBox<GwtGroup>();
        groupCombo.setStore(new ListStore<GwtGroup>());
        groupCombo.setFieldLabel(MSGS.deviceFormGroup());
        groupCombo.setForceSelection(true);
        groupCombo.setTypeAhead(false);
        groupCombo.setTriggerAction(TriggerAction.ALL);
        groupCombo.setAllowBlank(false);
        groupCombo.setEditable(false);
        groupCombo.setDisplayField("groupName");
        groupCombo.setValueField("id");

        gwtGroupService.findAll(currentSession.getSelectedAccount().getId(), new AsyncCallback<List<GwtGroup>>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught);                
            }

            @Override
            public void onSuccess(List<GwtGroup> result) {
                groupCombo.getStore().removeAll();
                groupCombo.getStore().add(NO_GROUP);
                groupCombo.getStore().add(result);
            }
        });
        fieldSet.add(groupCombo, formData);

        // Tag fieldset
        // FieldSet fieldSetTags = new FieldSet();
        // FormLayout layoutTags = new FormLayout();
        // layoutTags.setLabelWidth(Constants.LABEL_WIDTH_DEVICE_FORM);
        // fieldSetTags.setLayout(layoutTags);
        // fieldSetTags.setHeading(MSGS.deviceFormFieldsetTags());

        ContentPanel panel = new ContentPanel();
        panel.setBorders(false);
        panel.setBodyBorder(false);
        panel.setHeaderVisible(false);
        panel.setLayout(new RowLayout(Orientation.HORIZONTAL));
        panel.setBodyStyle("background-color:transparent");
        panel.setHeight(35);

        // Device Custom attributes fieldset
        FormLayout layoutSecurityOptions = new FormLayout();
        layoutSecurityOptions.setLabelWidth(Constants.LABEL_WIDTH_DEVICE_FORM);

        FieldSet fieldSetSecurityOptions = new FieldSet();
        fieldSetSecurityOptions.setLayout(layoutSecurityOptions);
        fieldSetSecurityOptions.setHeading(MSGS.deviceFormFieldsetSecurityOptions());

        deviceUserCouplingMode = new SimpleComboBox<String>();
        deviceUserCouplingMode.setName("devSecurityDeviceUserCouplingMode");
        deviceUserCouplingMode.setEditable(false);
        deviceUserCouplingMode.setTypeAhead(false);
        deviceUserCouplingMode.setAllowBlank(false);
        deviceUserCouplingMode.setFieldLabel(MSGS.deviceFormDeviceUserCouplingMode());
        deviceUserCouplingMode.setToolTip(MSGS.deviceFormDeviceUserCouplingModeTooltip());
        deviceUserCouplingMode.setTriggerAction(TriggerAction.ALL);

        fieldSetSecurityOptions.add(deviceUserCouplingMode, formData);

        deviceUserCouplingMode.add(GwtDeviceUserCouplingMode.INHERITED.getLabel());
        deviceUserCouplingMode.add(GwtDeviceUserCouplingMode.LOOSE.getLabel());
        deviceUserCouplingMode.add(GwtDeviceUserCouplingMode.STRICT.getLabel());
        //
        deviceUserCouplingMode.setSimpleValue(GwtDeviceUserCouplingMode.INHERITED.getLabel());

        reservedUserCombo = new ComboBox<GwtUser>();
        reservedUserCombo.setStore(new ListStore<GwtUser>());
        reservedUserCombo.setFieldLabel(MSGS.deviceFormDeviceUserCouplingReservedUser());
        reservedUserCombo.setToolTip(MSGS.deviceFormDeviceUserCouplingReservedUserTooltip());
        reservedUserCombo.setForceSelection(true);
        reservedUserCombo.setTypeAhead(false);
        reservedUserCombo.setTriggerAction(TriggerAction.ALL);
        reservedUserCombo.setAllowBlank(false);
        reservedUserCombo.setEditable(false);
        reservedUserCombo.setValueField("id");
        reservedUserCombo.setDisplayField("username");

        gwtUserService.findAll(currentSession.getSelectedAccount().getId(), new AsyncCallback<ListLoadResult<GwtUser>>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(ListLoadResult<GwtUser> result) {
                reservedUserCombo.getStore().removeAll();
                reservedUserCombo.getStore().add(NO_USER);
                reservedUserCombo.getStore().add(result.getData());
                
            }

        });

        fieldSetSecurityOptions.add(reservedUserCombo, formData);

        lastUserField = new TextField<String>();
        lastUserField.setName("devSecurityLastUser");
        lastUserField.setFieldLabel(MSGS.deviceFormDeviceUserCouplingLastUser());
        lastUserField.setToolTip(MSGS.deviceFormDeviceUserCouplingLastUserTooltip());
        lastUserField.setWidth(225);
        lastUserField.setReadOnly(true);
        fieldSetSecurityOptions.add(lastUserField, formData);

        // Device Custom attributes fieldset
        FieldSet fieldSetCustomAttributes = new FieldSet();
        FormLayout layoutCustomAttributes = new FormLayout();
        layoutCustomAttributes.setLabelWidth(Constants.LABEL_WIDTH_DEVICE_FORM);
        fieldSetCustomAttributes.setLayout(layoutCustomAttributes);
        fieldSetCustomAttributes.setHeading(MSGS.deviceFormFieldsetCustomAttributes());

        // Custom Attribute #1
        customAttribute1Field = new TextField<String>();
        customAttribute1Field.setName("customAttribute1");
        customAttribute1Field.setFieldLabel("* " + MSGS.deviceFormCustomAttribute1());
        customAttribute1Field.setWidth(225);
        fieldSetCustomAttributes.add(customAttribute1Field, formData);

        // Custom Attribute #2
        customAttribute2Field = new TextField<String>();
        customAttribute2Field.setName("customAttribute2");
        customAttribute2Field.setFieldLabel("* " + MSGS.deviceFormCustomAttribute2());
        customAttribute2Field.setWidth(225);
        fieldSetCustomAttributes.add(customAttribute2Field, formData);

        // Custom Attribute #3
        customAttribute3Field = new TextField<String>();
        customAttribute3Field.setName("customAttribute3");
        customAttribute3Field.setFieldLabel("* " + MSGS.deviceFormCustomAttribute3());
        customAttribute3Field.setWidth(225);
        fieldSetCustomAttributes.add(customAttribute3Field, formData);

        // Custom Attribute #4
        customAttribute4Field = new TextField<String>();
        customAttribute4Field.setName("customAttribute4");
        customAttribute4Field.setFieldLabel("* " + MSGS.deviceFormCustomAttribute4());
        customAttribute4Field.setWidth(225);
        fieldSetCustomAttributes.add(customAttribute4Field, formData);

        // Custom Attribute #5
        customAttribute5Field = new TextField<String>();
        customAttribute5Field.setName("customAttribute5");
        customAttribute5Field.setFieldLabel("* " + MSGS.deviceFormCustomAttribute5());
        customAttribute5Field.setWidth(225);
        fieldSetCustomAttributes.add(customAttribute5Field, formData);

        // Optlock
        optlock = new NumberField();
        optlock.setName("optlock");
        optlock.setEditable(false);
        optlock.setVisible(false);
        fieldSet.add(optlock, formData);

        formPanel.add(fieldSet);
        // m_formPanel.add(fieldSetTags);
        formPanel.add(fieldSetCustomAttributes);
        formPanel.add(fieldSetSecurityOptions);

        formPanel.setButtonAlign(HorizontalAlignment.CENTER);

        Button submitButton = new Button(MSGS.deviceFormSubmitButton());
        submitButton.addListener(Events.OnClick, new Listener<BaseEvent>() {

            @Override
            public void handleEvent(BaseEvent be) {
                // make sure all visible fields are valid before performing the action
                for (Field<?> field : formPanel.getFields()) {
                    if (field.isVisible() && !field.isValid()) {
                        MessageBox.alert(MSGS.error(), MSGS.formErrors(), null);
                        return;
                    }
                }

                if (selectedDevice == null) {

                    final GwtDeviceCreator gwtDeviceCreator = new GwtDeviceCreator();
                    gwtDeviceCreator.setScopeId(currentSession.getSelectedAccount().getId());

                    gwtDeviceCreator.setClientId(clientIdField.getValue());
                    gwtDeviceCreator.setGroupId(groupCombo.getValue().getId());
                    gwtDeviceCreator.setDisplayName(displayNameField.getValue());

                    // Security Options
                    GwtDeviceUserCouplingMode gwtDeviceUserCouplingMode = null;
                    if (deviceUserCouplingMode.getValue() != null) {
                        gwtDeviceUserCouplingMode = GwtDeviceUserCouplingMode.getEnumFromLabel(deviceUserCouplingMode.getValue().getValue());
                    }
                    gwtDeviceCreator.setDeviceUserCouplingMode(gwtDeviceUserCouplingMode);
                    gwtDeviceCreator.setReservedUserId(reservedUserCombo.getValue() != null ? reservedUserCombo.getValue().getId() : null);
                    // gwtDeviceCreator.setGwtPreferredUserId(deviceUserCombo.getValue().getId());

                    // Custom attributes
                    gwtDeviceCreator.setCustomAttribute1(unescapeValue(customAttribute1Field.getValue()));
                    gwtDeviceCreator.setCustomAttribute2(unescapeValue(customAttribute2Field.getValue()));
                    gwtDeviceCreator.setCustomAttribute3(unescapeValue(customAttribute3Field.getValue()));
                    gwtDeviceCreator.setCustomAttribute4(unescapeValue(customAttribute4Field.getValue()));
                    gwtDeviceCreator.setCustomAttribute5(unescapeValue(customAttribute5Field.getValue()));

                    //
                    // Getting XSRF token
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            gwtDeviceService.createDevice(token, gwtDeviceCreator, new AsyncCallback<GwtDevice>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                }

                                public void onSuccess(final GwtDevice gwtDevice) {
                                    //
                                    // Getting XSRF token
                                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                        @Override
                                        public void onFailure(Throwable ex) {
                                            FailureHandler.handle(ex);
                                        }

                                        @Override
                                        public void onSuccess(GwtXSRFToken token) {
                                            hide();
                                            ConsoleInfo.display(MSGS.info(), MSGS.deviceUpdateSuccess());
                                        }
                                    });

                                }
                            });
                        }
                    });
                }
                // Edit
                else {
                    // General info
                    selectedDevice.setDisplayName(unescapeValue(displayNameField.getValue()));
                    selectedDevice.setGwtDeviceStatus(statusCombo.getSimpleValue().name());
                    selectedDevice.setGroupId(groupCombo.getValue().getId());

                    // Security Options
                    String gwtDeviceUserCouplingMode = null;
                    if (deviceUserCouplingMode.getValue() != null) {
                        GwtDeviceUserCouplingMode tmp = GwtDeviceUserCouplingMode.getEnumFromLabel(deviceUserCouplingMode.getValue().getValue());
                        if (tmp != null) {
                            gwtDeviceUserCouplingMode = tmp.getLabel();
                        }

                    }
                    selectedDevice.setDeviceUserCouplingMode(gwtDeviceUserCouplingMode);
                    selectedDevice.setReservedUserId(reservedUserCombo.getValue() != null ? reservedUserCombo.getValue().getId() : null);
                    // m_selectedDevice.setCredentialsTight(GwtDeviceCredentialsTight.getEnumFromLabel(credentialsTightCombo.getSimpleValue()).name());
                    // m_selectedDevice.setCredentialsAllowChange(allowCredentialsChangeCheckbox.getValue());
                    // m_selectedDevice.setDeviceUserId(deviceUserCombo.getValue().getId());

                    // Custom attributes
                    selectedDevice.setCustomAttribute1(unescapeValue(customAttribute1Field.getValue()));
                    selectedDevice.setCustomAttribute2(unescapeValue(customAttribute2Field.getValue()));
                    selectedDevice.setCustomAttribute3(unescapeValue(customAttribute3Field.getValue()));
                    selectedDevice.setCustomAttribute4(unescapeValue(customAttribute4Field.getValue()));
                    selectedDevice.setCustomAttribute5(unescapeValue(customAttribute5Field.getValue()));

                    // Optlock
                    selectedDevice.setOptlock(optlock.getValue().intValue());

                    //
                    // Getting XSRF token
                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                        @Override
                        public void onFailure(Throwable ex) {
                            FailureHandler.handle(ex);
                        }

                        @Override
                        public void onSuccess(GwtXSRFToken token) {
                            gwtDeviceService.updateAttributes(token, selectedDevice, new AsyncCallback<GwtDevice>() {

                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                }

                                public void onSuccess(GwtDevice gwtDevice) {
                                    //
                                    // Getting XSRF token
                                    gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                                        @Override
                                        public void onFailure(Throwable ex) {
                                            FailureHandler.handle(ex);
                                        }

                                        @Override
                                        public void onSuccess(GwtXSRFToken token) {
                                            hide();
                                            ConsoleInfo.display(MSGS.info(), selectedDevice == null ? MSGS.deviceCreationSuccess() : MSGS.deviceUpdateSuccess());
                                        }
                                    });

                                }
                            });
                        }
                    });
                }
            }
        });

        Button cancelButton = new Button(MSGS.deviceFormCancelButton());
        cancelButton.addListener(Events.OnClick, new Listener<BaseEvent>() {

            @Override
            public void handleEvent(BaseEvent be) {
                hide();
            }
        });

        formPanel.addButton(submitButton);
        formPanel.addButton(cancelButton);

        add(formPanel);

        // Hide components according to NEW/EDIT mode
        makeNewEditAppearance();

        // Populate fields if we are in EDIT mode
        if (selectedDevice != null) {
            populateFields();
        }
    }

    private void populateFields() {
        if (selectedDevice != null) {
            // General info data
            clientIdLabel.setText(selectedDevice.getClientId());
            displayNameField.setValue(selectedDevice.getUnescapedDisplayName());
            statusCombo.setSimpleValue(GwtDeviceQueryPredicates.GwtDeviceStatus.valueOf(selectedDevice.getGwtDeviceStatus()));

            // Security options data
            GwtDeviceUserCouplingMode gwtDeviceUserCouplingMode = null;
            if (selectedDevice.getDeviceUserCouplingMode() != null) {
                gwtDeviceUserCouplingMode = GwtDeviceUserCouplingMode.getEnumFromLabel(selectedDevice.getDeviceUserCouplingMode());
            }
            deviceUserCouplingMode.setSimpleValue(gwtDeviceUserCouplingMode != null ? gwtDeviceUserCouplingMode.getLabel() : "N/A");
            if (selectedDevice.getReservedUserId() != null) {
                gwtUserService.find(currentSession.getSelectedAccount().getId(), selectedDevice.getReservedUserId(), new AsyncCallback<GwtUser>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(GwtUser gwtUser) {
                        reservedUserCombo.setValue(gwtUser);
                    }
                });
            } else {
                reservedUserCombo.setValue(NO_USER);
            }
            if (selectedDevice.getLastUserId() != null) {
                gwtUserService.find(currentSession.getSelectedAccount().getId(), selectedDevice.getLastUserId(), new AsyncCallback<GwtUser>() {
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                    }
                    
                    @Override
                    public void onSuccess(GwtUser gwtUser) {
                        if (gwtUser != null) {
                            lastUserField.setValue(gwtUser.getUsername());
                        } else {
                            lastUserField.setValue("N/A");
                        }
                    }
                });
            }
            else {
                lastUserField.setValue("N/A");
            }
            if (selectedDevice.getGroupId() != null) {
                gwtGroupService.find(currentSession.getSelectedAccount().getId(), selectedDevice.getGroupId(), new AsyncCallback<GwtGroup>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                    }

                    @Override
                    public void onSuccess(GwtGroup result) {
                        groupCombo.setValue(result);
                    }
                });
            } else {
                groupCombo.setValue(NO_GROUP);
            }
//            // Custom attributes data
            customAttribute1Field.setValue(selectedDevice.getUnescapedCustomAttribute1());
            customAttribute2Field.setValue(selectedDevice.getUnescapedCustomAttribute2());
            customAttribute4Field.setValue(selectedDevice.getUnescapedCustomAttribute4());
            customAttribute3Field.setValue(selectedDevice.getUnescapedCustomAttribute3());
            customAttribute5Field.setValue(selectedDevice.getUnescapedCustomAttribute5());

            // Other data
            optlock.setValue(selectedDevice.getOptlock());
        }
    }

    private void makeNewEditAppearance() {
        // New
        if (selectedDevice == null) {
            clientIdLabel.hide();
            statusCombo.hide();
            // allowCredentialsChangeCheckbox.hide();
        }
        // Edit
        else {
            clientIdField.hide();

            // if (m_selectedDevice.getCredentialTightEnum().equals(GwtDeviceCredentialsTight.LOOSE)) {
            // allowCredentialsChangeCheckbox.hide();
            // }
            // else {

            // if (m_selectedDevice.getCredentialTightEnum().equals(GwtDeviceCredentialsTight.INHERITED)) {
            // allowCredentialsChangeCheckbox.hide();
            // }
            // }
        }
    }

    private String unescapeValue(String value) {
        return KapuaSafeHtmlUtils.htmlUnescape(value);
    }
}
