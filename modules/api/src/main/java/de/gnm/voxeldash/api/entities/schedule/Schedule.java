package de.gnm.voxeldash.api.entities.schedule;

import java.util.ArrayList;
import java.util.List;

public class Schedule {

    private int id;
    private String name;
    private ScheduleInterval interval;
    private int intervalValue; // For HOURLY: minute (0-59), DAILY: hour (0-23), WEEKLY: day of week (0-6, 0=Sunday)
    private int timeValue; // For DAILY/WEEKLY: minute of the hour (0-59)
    private boolean enabled;
    private long lastRun;
    private List<ScheduleTask> tasks;

    public Schedule() {
        this.tasks = new ArrayList<>();
        this.enabled = true;
    }

    public Schedule(int id, String name, ScheduleInterval interval, int intervalValue, int timeValue, boolean enabled, long lastRun) {
        this.id = id;
        this.name = name;
        this.interval = interval;
        this.intervalValue = intervalValue;
        this.timeValue = timeValue;
        this.enabled = enabled;
        this.lastRun = lastRun;
        this.tasks = new ArrayList<>();
    }

    /**
     * Gets the unique identifier of the schedule
     *
     * @return the schedule ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the schedule
     *
     * @param id the schedule ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the name of the schedule
     *
     * @return the schedule name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the schedule
     *
     * @param name the schedule name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the interval type of the schedule
     *
     * @return the interval type
     */
    public ScheduleInterval getInterval() {
        return interval;
    }

    /**
     * Sets the interval type of the schedule
     *
     * @param interval the interval type
     */
    public void setInterval(ScheduleInterval interval) {
        this.interval = interval;
    }

    /**
     * Gets the interval value
     * For HOURLY: minute of the hour (0-59)
     * For DAILY: hour of the day (0-23)
     * For WEEKLY: day of the week (0-6, where 0 is Sunday)
     *
     * @return the interval value
     */
    public int getIntervalValue() {
        return intervalValue;
    }

    /**
     * Sets the interval value
     *
     * @param intervalValue the interval value
     */
    public void setIntervalValue(int intervalValue) {
        this.intervalValue = intervalValue;
    }

    /**
     * Gets the time value (minute of the hour for DAILY/WEEKLY schedules)
     *
     * @return the time value
     */
    public int getTimeValue() {
        return timeValue;
    }

    /**
     * Sets the time value
     *
     * @param timeValue the time value
     */
    public void setTimeValue(int timeValue) {
        this.timeValue = timeValue;
    }

    /**
     * Checks if the schedule is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the schedule is enabled
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the timestamp of the last run
     *
     * @return the last run timestamp in milliseconds
     */
    public long getLastRun() {
        return lastRun;
    }

    /**
     * Sets the timestamp of the last run
     *
     * @param lastRun the last run timestamp in milliseconds
     */
    public void setLastRun(long lastRun) {
        this.lastRun = lastRun;
    }

    /**
     * Gets the list of tasks associated with this schedule
     *
     * @return the list of tasks
     */
    public List<ScheduleTask> getTasks() {
        return tasks;
    }

    /**
     * Sets the list of tasks associated with this schedule
     *
     * @param tasks the list of tasks
     */
    public void setTasks(List<ScheduleTask> tasks) {
        this.tasks = tasks;
    }

    /**
     * Adds a task to this schedule
     *
     * @param task the task to add
     */
    public void addTask(ScheduleTask task) {
        this.tasks.add(task);
    }

    /**
     * Gets a human-readable description of the schedule timing
     *
     * @return the timing description
     */
    public String getTimingDescription() {
        switch (interval) {
            case HOURLY:
                return String.format("Every hour at %02d minutes", intervalValue);
            case DAILY:
                return String.format("Every day at %02d:%02d", intervalValue, timeValue);
            case WEEKLY: {
                String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                return String.format("Every week on %s at %02d:%02d", days[intervalValue % 7], timeValue / 60, timeValue % 60);
            }
            default:
                return "";
        }
    }
}
