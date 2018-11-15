package org.usac.bots.jbot.entities;

import org.joda.time.DateTime;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.usac.bots.jbot.scheduler.Scheduler;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import static org.usac.bots.jbot.util.DateUtils.fullFormat;

public class ScheduledTask extends SerializableItem implements Trigger {

    private int interval;
    private Date startDate;
    private Date endDate;
    private Date lastExecutionDate;
    private TaskSchedule taskSchedule;
    private ChatMessage message;
    private boolean endOnSuccess;
    private boolean endOnFailure;
    private String endOnStatus;
    private transient Runnable runnable;
    private transient ScheduledFuture<?> future;
    private transient Scheduler scheduler;

    @Override
    public Date nextExecutionTime(TriggerContext triggerContext) {
        DateTime result = null;

        if (triggerContext.lastActualExecutionTime() == null) {
            if (startDate != null) {
                result = new DateTime(startDate).plusSeconds(interval);
            } else {
                result = new DateTime().plusSeconds(interval);
            }
        } else {
            switch (taskSchedule) {
                case INTERVAL:
                    lastExecutionDate = triggerContext.lastActualExecutionTime();
                    result = new DateTime(triggerContext.lastActualExecutionTime()).plusSeconds(interval);
                    break;
                case DELAY:
                    result = null;
                    break;
            }
        }
        if (endDate != null && result != null && new DateTime(endDate).isBefore(result)) {
            result = null;
        }

        Date resultDate = result != null ? result.toDate() : null;

        return resultDate;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(">Task: \"").append(message.getProcessedText()).append("\"  \n");

        switch (taskSchedule) {
            case DELAY:
                result.append(">Type: Delayed by ").append(interval).append(" seconds after ").append(fullFormat.format(startDate)).append("  \n");
                break;
            case INTERVAL:
                result.append(">Type: Repeating every ").append(interval).append(" seconds  after ").append(fullFormat.format(startDate)).append("  \n");
        }

        result.append(">Next Run: ").append(fullFormat.format(new DateTime(lastExecutionDate).plusSeconds(interval).toDate())).append("  \n");
        result.append(">Say \"cancel scheduled ").append(alias).append("\" to cancel execution  \n");

        if (endDate != null) {
            result.append(">Repeats until ").append(endDate).append("  \n");
        }
        if (endOnSuccess) {
            result.append(">This task will run until it succeeds  \n");
        }
        if (endOnFailure) {
            result.append(">This task will run until it fails  \n");
        }
        if (endOnStatus != null && !endOnStatus.isEmpty()) {
            result.append(">This task will run until the result status becomes '"+endOnStatus+"'  \n");
        }


        return result.toString();
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }


    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setInterval(int interval, Date startDate, Date endDate) {
        this.interval = interval;
        this.startDate = startDate;
        this.endDate = endDate;
        taskSchedule = TaskSchedule.INTERVAL;
    }

    public int getDelay() {
        return interval;
    }

    public void setDelay(int interval) {
        this.interval = interval;
        this.startDate = new Date();
        this.endDate = null;
        taskSchedule = TaskSchedule.DELAY;
    }

    public TaskSchedule getTaskSchedule() {
        return taskSchedule;
    }

    public void setTaskSchedule(TaskSchedule taskSchedule) {
        this.taskSchedule = taskSchedule;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public boolean isEndOnSuccess() {
        return endOnSuccess;
    }

    public void setEndOnSuccess(boolean endOnSuccess) {
        this.endOnSuccess = endOnSuccess;
    }

    public boolean isEndOnFailure() {
        return endOnFailure;
    }

    public void setEndOnFailure(boolean endOnFailure) {
        this.endOnFailure = endOnFailure;
    }

    public void setEndOnStatus(String endOnStatus) {
        this.endOnStatus = endOnStatus;
    }

    public String getEndOnStatus() {
        return endOnStatus;
    }

    public enum TaskSchedule {
        INTERVAL,
        DELAY
    }
}
