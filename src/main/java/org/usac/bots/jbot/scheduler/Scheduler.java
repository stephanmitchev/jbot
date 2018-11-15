package org.usac.bots.jbot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.usac.bots.jbot.entities.HandlerResult;
import org.usac.bots.jbot.entities.ScheduledTask;
import org.usac.bots.jbot.datasources.FileDataSource;
import org.usac.bots.jbot.messagehandlers.MessageHandlers;
import org.usac.bots.jbot.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class Scheduler {

    private Map<String, ScheduledTask> tasks;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final FileDataSource fileDataSource;
    private final MessageHandlers messageHandlers;

    private static Logger log = null;


    @Autowired
    public Scheduler(ThreadPoolTaskScheduler taskScheduler, FileDataSource fileDataSource, MessageHandlers messageHandlers) {
        log = LoggerFactory.getLogger(this.getClass());

        this.taskScheduler = taskScheduler;
        this.fileDataSource = fileDataSource;
        this.messageHandlers = messageHandlers;

        tasks = new HashMap<>();

        loadAllSavedTasks();
    }

    private void loadAllSavedTasks() {
        List<ScheduledTask> tasks = fileDataSource.loadObjects(null, new ScheduledTask(), ScheduledTask.class);

        for (ScheduledTask task : tasks) {
            register(task);
        }

        log.info("Loaded " + tasks.size() + " scheduled tasks");
    }

    public ScheduledTask register(ScheduledTask task) {

        // Do not regenerate the Alias if there already as it will duplicate the tasks on disk
        if (task.getAlias() == null || task.getAlias().isEmpty()) {
            task.setAlias(StringUtils.getUniqueId(4, tasks.keySet()));
        }

        task.setScheduler(this);

        task.setRunnable(() -> {
                    HandlerResult result = messageHandlers.handleChatMessage(task.getMessage(), messageHandlers.getHandler("scheduling"));
                    if (task.getTaskSchedule() == ScheduledTask.TaskSchedule.DELAY
                            || (result.isSuccessful() && task.isEndOnSuccess())
                            || (!result.isSuccessful() && task.isEndOnFailure())
                            || (task.getEndOnStatus() != null && !task.getEndOnStatus().isEmpty() && task.getEndOnStatus().equalsIgnoreCase(result.getStatus()))
                    ) {
                        messageHandlers.getScheduler().unregister(task);
                    }
                }
        );
        task.setFuture(schedule(task.getRunnable(), task));
        fileDataSource.saveObject(task.getOwner(), task, task.getAlias(), ScheduledTask.class);

        tasks.put(task.getAlias(), task);

        return task;
    }

    public void unregister(ScheduledTask task) {
        task.getFuture().cancel(true);
        fileDataSource.deleteObject(task.getOwner(), task.getAlias(), ScheduledTask.class);
        tasks.remove(task.getAlias());
    }

    private ScheduledFuture<?> schedule(Runnable runnable, Trigger trigger) {
        return taskScheduler.schedule(runnable, trigger);
    }


    public Map<String, ScheduledTask> getTasks() {

        return tasks;
    }

}
