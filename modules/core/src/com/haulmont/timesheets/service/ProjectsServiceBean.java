/*
 * Copyright (c) 2015 com.haulmont.ts.service
 */
package com.haulmont.timesheets.service;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.timesheets.SystemDataManager;
import com.haulmont.timesheets.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * @author gorelov
 */
@Service(ProjectsService.NAME)
public class ProjectsServiceBean implements ProjectsService {
    @Inject
    protected DataManager dataManager;

    @Inject
    protected SystemDataManager systemDataManager;

    protected List<Project> getAllProjects() {
        LoadContext loadContext = new LoadContext(Project.class)
                .setView("project-full");
        loadContext.setQueryString("select e from ts$Project e");

        return dataManager.loadList(loadContext);
    }


    @Override
    @Transactional
    public List<Project> getChildren(Project parent) {
        List<Project> projects = getAllProjects();
        if (!projects.isEmpty()) {
            List<Project> children = new ArrayList<>();
            for (Project project : projects) {
                if (parent.equals(project.getParent())) {
                    children.add(project);
                    children.addAll(getChildren(project));
                }
            }
            return children;
        }
        return Collections.emptyList();
    }

    @Override
    public void setClient(Project project, @Nullable Client client) {
        project.setClient(client);
        dataManager.commit(project);
    }

    @Nullable
    @Override
    public ProjectRole getUserProjectRole(Project project, User user) {
        LoadContext loadContext = new LoadContext(ProjectParticipant.class)
                .setView("projectParticipant-full");
        loadContext.setQueryString("select e from ts$ProjectParticipant e where e.user.id = :userId and e.project.id = :projectId")
                .setParameter("userId", user.getId())
                .setParameter("projectId", project.getId());
        ProjectParticipant participant = dataManager.load(loadContext);
        return participant != null ? participant.getRole() : null;
    }

    @Nullable
    @Override
    public <T extends Entity> T getEntityByCode(Class<T> clazz, String code, String viewName) {
        return systemDataManager.getEntityByCode(clazz, code, viewName);
    }

    @Override
    public List<TimeEntry> getTimeEntriesForPeriod(Date start, Date end, User user, @Nullable TimeEntryStatus status) {
        LoadContext loadContext = new LoadContext(TimeEntry.class)
                .setView("timeEntry-full");
        String queryStr = "select e from ts$TimeEntry e where e.user.id = :userId and (e.date between :start and :end)";
        if (status != null) {
            queryStr += " and e.status = :status";
        }
        LoadContext.Query query = loadContext.setQueryString(queryStr)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("userId", user.getId());
        if (status != null) {
            query.setParameter("status", status.getId());
        }
        return dataManager.loadList(loadContext);
    }

    @Override
    public List<TimeEntry> getApprovableTimeEntriesForPeriod(Date start, Date end, User approver, User user, @Nullable TimeEntryStatus status) {
        LoadContext loadContext = new LoadContext(TimeEntry.class)
                .setView("timeEntry-full");
        String queryStr = "select e from ts$TimeEntry e join e.task t join t.project pr join pr.participants p " +
                "where p.user.id = :approverId and (p.role.code = 'manager' or p.role.code = 'approver') " +
                "and e.user.id = :userId and (e.date between :start and :end)";
        if (status != null) {
            queryStr += " and e.status = :status";
        }
        LoadContext.Query query = loadContext.setQueryString(queryStr)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("approverId", approver.getId())
                .setParameter("userId", user.getId());
        if (status != null) {
            query.setParameter("status", status.getId());
        }
        return dataManager.loadList(loadContext);
    }

    @Override
    public List<TimeEntry> getTimeEntriesForUser(User user) {
        LoadContext loadContext = new LoadContext(TimeEntry.class)
                .setView("timeEntry-full");
        loadContext.setQueryString("select e from ts$TimeEntry e where e.user.id = :userId")
                .setParameter("userId", user.getId());
        return dataManager.loadList(loadContext);
    }

    @Override
    public List<Holiday> getHolidays() {
        LoadContext loadContext = new LoadContext(Holiday.class);
        loadContext.setQueryString("select e from ts$Holiday e");
        return dataManager.loadList(loadContext);
    }

    @Override
    public void removeTimeEntry(TimeEntry timeEntry) {
        CommitContext commitContext = new CommitContext();
        commitContext.getRemoveInstances().add(timeEntry);
        dataManager.commit(commitContext);
    }

    @Override
    public void removeTimeEntries(List<TimeEntry> timeEntries) {
        CommitContext commitContext = new CommitContext();
        commitContext.getRemoveInstances().addAll(timeEntries);
        dataManager.commit(commitContext);
    }

    @Override
    public void updateTimeEntriesStatus(List<TimeEntry> timeEntries, TimeEntryStatus status) {
        CommitContext commitContext = new CommitContext();
        for (TimeEntry entry : timeEntries) {
            entry.setStatus(status);
            commitContext.getCommitInstances().add(entry);
        }
        dataManager.commit(commitContext);
    }

    @Override
    public List<Task> getActiveTasksForUser(User user) {
        LoadContext loadContext = new LoadContext(Task.class)
                .setView("task-full");
        loadContext.setQueryString("select e from ts$Task e join e.participants p " +
                "where p.user.id = :userId and e.status = 'active' order by e.project")
                .setParameter("userId", user.getId());
        List<Task> assignedTasks = dataManager.loadList(loadContext);
        loadContext.setQueryString("select e from ts$Task e join e.project pr join pr.participants p " +
                "where p.user.id = :userId and e.participants is null and e.status = 'active' order by e.project")
                .setParameter("userId", user.getId());
        List<Task> commonTasks = dataManager.loadList(loadContext);
        if (assignedTasks.isEmpty() && commonTasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<Task> allTasks = new ArrayList<>(assignedTasks.size() + commonTasks.size());
        allTasks.addAll(assignedTasks);
        allTasks.addAll(commonTasks);
        return allTasks;
    }

    @Override
    public Map<String, Task> getActiveTasksForUserAndProject(User user, Project project) {
        LoadContext loadContext = new LoadContext(Task.class)
                .setView("task-full");
        loadContext.setQueryString("select e from ts$Task e join e.participants p " +
                "where p.user.id = :userId and e.project.id = :projectId and e.status = 'active' order by e.project")
                .setParameter("projectId", project.getId())
                .setParameter("userId", user.getId());
        List<Task> assignedTasks = dataManager.loadList(loadContext);
        loadContext.setQueryString("select e from ts$Task e join e.project pr join pr.participants p " +
                "where p.user.id = :userId and e.project.id = :projectId and e.participants is null and e.status = 'active' order by e.project")
                .setParameter("projectId", project.getId())
                .setParameter("userId", user.getId());
        List<Task> commonTasks = dataManager.loadList(loadContext);
        if (assignedTasks.isEmpty() && commonTasks.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Task> allTasks = new ArrayList<>(assignedTasks.size() + commonTasks.size());
        allTasks.addAll(assignedTasks);
        allTasks.addAll(commonTasks);
        Map<String, Task> tasksMap = new HashMap<>(allTasks.size());
        for (Task task : allTasks) {
            tasksMap.put(task.getName(), task);
        }
        return tasksMap;
    }

    public List<Project> getActiveProjectsForUser(User user) {
        LoadContext loadContext = new LoadContext(Project.class)
                .setView(View.LOCAL);
        LoadContext.Query query =
                new LoadContext.Query("select pr from ts$Project pr, in(pr.participants) p " +
                        "where p.user.id = :userId and pr.status = 'open'")
                        .setParameter("userId", user.getId());
        loadContext.setQuery(query);
        return dataManager.loadList(loadContext);
    }
}