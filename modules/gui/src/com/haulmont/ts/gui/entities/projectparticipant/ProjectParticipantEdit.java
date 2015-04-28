/*
 * Copyright (c) 2015 com.haulmont.ts.gui.entities.projectparticipant
 */
package com.haulmont.ts.gui.entities.projectparticipant;

import com.haulmont.cuba.gui.DialogParams;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.LookupPickerField;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.ts.entity.Project;
import com.haulmont.ts.entity.ProjectParticipant;

import javax.inject.Named;
import java.util.*;

/**
 * @author gorelov
 */
public class ProjectParticipantEdit extends AbstractEditor<ProjectParticipant> {

    @Named("fieldGroup.user")
    private LookupPickerField userField;
    @Named("fieldGroup.project")
    private PickerField projectField;

    @Override
    public void init(Map<String, Object> params) {
        userField.addAction(createLookupAction(userField));
        userField.addAction(new PickerField.ClearAction(userField));

        final UniqueUserValidator validator = new UniqueUserValidator();
        userField.addValidator(validator);

        @SuppressWarnings("unchecked")
        Collection<User> assignedUsers = (Collection<User>) params.get("assignedUsers");
        if (assignedUsers != null)
            validator.setAssignedUsers(assignedUsers);

        projectField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (value != null) {
                    if (!value.equals(prevValue)) {
                        Project project = (Project) value;
                        validator.setAssignedUsers(getAssignedUsers(project));
                    }
                } else {
                    validator.setAssignedUsers(null);
                }
            }
        });
    }

    @Override
    protected void postInit() {
        super.postInit();
        ProjectParticipant participant = getItem();
        if (participant.getProject() != null) {
            projectField.setEnabled(false);
        }
    }

    private PickerField.LookupAction createLookupAction(PickerField pickerField) {
        PickerField.LookupAction lookupAction = new PickerField.LookupAction(pickerField);
        lookupAction.setLookupScreenOpenType(WindowManager.OpenType.DIALOG);
        lookupAction.setLookupScreenDialogParams(new DialogParams()
                .setWidth(800)
                .setHeight(500)
                .setResizable(true));
        return lookupAction;
    }

    public Collection<User> getAssignedUsers(Project project) {
        Set<ProjectParticipant> participants = project.getParticipants();
        if (participants.size() > 0) {
            List<User> assignedUsers = new ArrayList<>(participants.size());
            for (ProjectParticipant participant : participants)
                assignedUsers.add(participant.getUser());
            return assignedUsers;
        }
        return Collections.emptyList();
    }
}