package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.FieldType;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.controller.ActionRegistry;
import de.gnm.voxeldash.api.controller.ScheduleController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.schedule.Schedule;
import de.gnm.voxeldash.api.entities.schedule.ScheduleAction;
import de.gnm.voxeldash.api.entities.schedule.ScheduleInterval;
import de.gnm.voxeldash.api.entities.schedule.ScheduleTask;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.http.Response;

import java.util.List;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class ScheduleRouter extends BaseRoute {

    @ApiDoc(summary = "List schedule actions", description = "Returns all available schedule actions registered in the action registry.", tag = "Schedules")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Schedules)
    @Path("/schedules/actions")
    @Method(GET)
    public Response listActions() {
        ActionRegistry registry = getLoader().getActionRegistry();
        
        ArrayNode actionsArray = getMapper().createArrayNode();
        for (ScheduleAction action : registry.getAllActions()) {
            actionsArray.add(actionToJson(action));
        }

        return new JSONResponse().add("actions", actionsArray);
    }

    @ApiDoc(summary = "List schedules", description = "Returns all configured schedules including their tasks.", tag = "Schedules")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Schedules)
    @Path("/schedules")
    @Method(GET)
    public Response listSchedules() {
        ScheduleController controller = getController(ScheduleController.class);
        List<Schedule> schedules = controller.getAllSchedules();

        ArrayNode schedulesArray = getMapper().createArrayNode();
        for (Schedule schedule : schedules) {
            schedulesArray.add(scheduleToJson(schedule));
        }

        return new JSONResponse().add("schedules", schedulesArray);
    }

    @ApiDoc(summary = "Get a schedule", description = "Returns a single schedule by its ID, including its tasks.", tag = "Schedules")
    @ApiField(name = "id", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule to retrieve")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Schedules)
    @Path("/schedules/:id")
    @Method(GET)
    public Response getSchedule(RawRequest request) {
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        ScheduleController controller = getController(ScheduleController.class);
        Schedule schedule = controller.getSchedule(id);

        if (schedule == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        return new JSONResponse().add("schedule", scheduleToJson(schedule));
    }

    @ApiDoc(summary = "Create a schedule", description = "Creates a new schedule with the given name and interval. Supports HOURLY, DAILY and WEEKLY intervals.", tag = "Schedules")
    @ApiField(name = "name", description = "Display name of the schedule")
    @ApiField(name = "interval", description = "Interval type: HOURLY, DAILY or WEEKLY")
    @ApiField(name = "intervalValue", type = FieldType.INTEGER, description = "Interval value; meaning depends on the interval type (e.g. minute, hour or day of week)")
    @ApiField(name = "timeValue", type = FieldType.INTEGER, required = false, description = "Additional time value used by DAILY and WEEKLY intervals")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules")
    @Method(POST)
    public Response createSchedule(JSONRequest request) {
        request.checkFor("name", "interval", "intervalValue");

        String name = request.get("name");
        String intervalStr = request.get("interval");
        int intervalValue = request.getInt("intervalValue");
        int timeValue = request.has("timeValue") ? request.getInt("timeValue") : 0;

        ScheduleInterval interval = ScheduleInterval.fromString(intervalStr);
        if (interval == null) {
            return new JSONResponse().error("Invalid interval type. Must be HOURLY, DAILY, or WEEKLY");
        }

        String validationError = validateIntervalValue(interval, intervalValue, timeValue);
        if (validationError != null) {
            return new JSONResponse().error(validationError);
        }

        ScheduleController controller = getController(ScheduleController.class);
        int id = controller.createSchedule(name, interval, intervalValue, timeValue);

        if (id == -1) {
            return new JSONResponse().error("Failed to create schedule");
        }

        Schedule schedule = controller.getSchedule(id);
        return new JSONResponse().add("schedule", scheduleToJson(schedule));
    }

    @ApiDoc(summary = "Update a schedule", description = "Updates the name and interval configuration of an existing schedule.", tag = "Schedules")
    @ApiField(name = "id", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule to update")
    @ApiField(name = "name", description = "Display name of the schedule")
    @ApiField(name = "interval", description = "Interval type: HOURLY, DAILY or WEEKLY")
    @ApiField(name = "intervalValue", type = FieldType.INTEGER, description = "Interval value; meaning depends on the interval type (e.g. minute, hour or day of week)")
    @ApiField(name = "timeValue", type = FieldType.INTEGER, required = false, description = "Additional time value used by DAILY and WEEKLY intervals")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:id")
    @Method(PUT)
    public Response updateSchedule(JSONRequest request) {
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        request.checkFor("name", "interval", "intervalValue");

        String name = request.get("name");
        String intervalStr = request.get("interval");
        int intervalValue = request.getInt("intervalValue");
        int timeValue = request.has("timeValue") ? request.getInt("timeValue") : 0;

        ScheduleInterval interval = ScheduleInterval.fromString(intervalStr);
        if (interval == null) {
            return new JSONResponse().error("Invalid interval type. Must be HOURLY, DAILY, or WEEKLY");
        }

        String validationError = validateIntervalValue(interval, intervalValue, timeValue);
        if (validationError != null) {
            return new JSONResponse().error(validationError);
        }

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getSchedule(id) == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        if (!controller.updateSchedule(id, name, interval, intervalValue, timeValue)) {
            return new JSONResponse().error("Failed to update schedule");
        }

        Schedule schedule = controller.getSchedule(id);
        return new JSONResponse().add("schedule", scheduleToJson(schedule));
    }

    @ApiDoc(summary = "Delete a schedule", description = "Deletes the schedule with the given ID.", tag = "Schedules")
    @ApiField(name = "id", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule to delete")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:id")
    @Method(DELETE)
    public Response deleteSchedule(RawRequest request) {
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getSchedule(id) == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        if (!controller.deleteSchedule(id)) {
            return new JSONResponse().error("Failed to delete schedule");
        }

        return new JSONResponse().message("Schedule deleted successfully");
    }

    @ApiDoc(summary = "Toggle a schedule", description = "Enables or disables the schedule with the given ID.", tag = "Schedules")
    @ApiField(name = "id", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule to toggle")
    @ApiField(name = "enabled", type = FieldType.BOOLEAN, description = "Whether the schedule should be enabled")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:id/toggle")
    @Method(POST)
    public Response toggleSchedule(JSONRequest request) {
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        request.checkFor("enabled");
        boolean enabled = request.getBoolean("enabled");

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getSchedule(id) == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        if (!controller.setScheduleEnabled(id, enabled)) {
            return new JSONResponse().error("Failed to update schedule");
        }

        return new JSONResponse().message(enabled ? "Schedule enabled" : "Schedule disabled");
    }


    @ApiDoc(summary = "List schedule tasks", description = "Returns all tasks belonging to the given schedule.", tag = "Schedules")
    @ApiField(name = "scheduleId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule whose tasks to list")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Schedules)
    @Path("/schedules/:scheduleId/tasks")
    @Method(GET)
    public Response listTasks(RawRequest request) {
        int scheduleId;
        try {
            scheduleId = Integer.parseInt(request.getParameter("scheduleId"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getSchedule(scheduleId) == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        List<ScheduleTask> tasks = controller.getTasksForSchedule(scheduleId);
        ArrayNode tasksArray = getMapper().createArrayNode();
        for (ScheduleTask task : tasks) {
            tasksArray.add(taskToJson(task));
        }

        return new JSONResponse().add("tasks", tasksArray);
    }

    @ApiDoc(summary = "Create a schedule task", description = "Adds a new task to the given schedule using the specified action.", tag = "Schedules")
    @ApiField(name = "scheduleId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule to add the task to")
    @ApiField(name = "actionId", description = "ID of a registered action to run for this task")
    @ApiField(name = "metadata", required = false, description = "Optional metadata passed to the action")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:scheduleId/tasks")
    @Method(POST)
    public Response createTask(JSONRequest request) {
        int scheduleId;
        try {
            scheduleId = Integer.parseInt(request.getParameter("scheduleId"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid schedule ID");
        }

        request.checkFor("actionId");

        String actionId = request.get("actionId");
        String metadata = request.has("metadata") ? request.get("metadata") : "";

        ActionRegistry registry = getLoader().getActionRegistry();
        if (!registry.hasAction(actionId)) {
            return new JSONResponse().error("Invalid action type: " + actionId);
        }

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getSchedule(scheduleId) == null) {
            return new JSONResponse().error("Schedule not found", 404);
        }

        int executionOrder = controller.getNextExecutionOrder(scheduleId);
        int id = controller.createTask(scheduleId, actionId, metadata, executionOrder);

        if (id == -1) {
            return new JSONResponse().error("Failed to create task");
        }

        ScheduleTask task = controller.getTask(id);
        return new JSONResponse().add("task", taskToJson(task));
    }

    @ApiDoc(summary = "Update a schedule task", description = "Updates the action, metadata and execution order of an existing task.", tag = "Schedules")
    @ApiField(name = "scheduleId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule the task belongs to")
    @ApiField(name = "taskId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the task to update")
    @ApiField(name = "actionId", description = "ID of a registered action to run for this task")
    @ApiField(name = "metadata", required = false, description = "Optional metadata passed to the action")
    @ApiField(name = "executionOrder", type = FieldType.INTEGER, required = false, description = "Order in which this task is executed within the schedule")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:scheduleId/tasks/:taskId")
    @Method(PUT)
    public Response updateTask(JSONRequest request) {
        int taskId;
        try {
            taskId = Integer.parseInt(request.getParameter("taskId"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid task ID");
        }

        request.checkFor("actionId");

        String actionId = request.get("actionId");
        String metadata = request.has("metadata") ? request.get("metadata") : "";
        int executionOrder = request.has("executionOrder") ? request.getInt("executionOrder") : 0;

        ActionRegistry registry = getLoader().getActionRegistry();
        if (!registry.hasAction(actionId)) {
            return new JSONResponse().error("Invalid action type: " + actionId);
        }

        ScheduleController controller = getController(ScheduleController.class);
        ScheduleTask existingTask = controller.getTask(taskId);
        
        if (existingTask == null) {
            return new JSONResponse().error("Task not found", 404);
        }

        if (!controller.updateTask(taskId, actionId, metadata, executionOrder)) {
            return new JSONResponse().error("Failed to update task");
        }

        ScheduleTask task = controller.getTask(taskId);
        return new JSONResponse().add("task", taskToJson(task));
    }

    @ApiDoc(summary = "Delete a schedule task", description = "Deletes the task with the given ID from the schedule.", tag = "Schedules")
    @ApiField(name = "scheduleId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the schedule the task belongs to")
    @ApiField(name = "taskId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the task to delete")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Schedules, level = PermissionLevel.FULL)
    @Path("/schedules/:scheduleId/tasks/:taskId")
    @Method(DELETE)
    public Response deleteTask(RawRequest request) {
        int taskId;
        try {
            taskId = Integer.parseInt(request.getParameter("taskId"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid task ID");
        }

        ScheduleController controller = getController(ScheduleController.class);
        
        if (controller.getTask(taskId) == null) {
            return new JSONResponse().error("Task not found", 404);
        }

        if (!controller.deleteTask(taskId)) {
            return new JSONResponse().error("Failed to delete task");
        }

        return new JSONResponse().message("Task deleted successfully");
    }

    private String validateIntervalValue(ScheduleInterval interval, int intervalValue, int timeValue) {
        switch (interval) {
            case HOURLY:
                if (intervalValue < 0 || intervalValue > 59) {
                    return "For HOURLY interval, intervalValue must be 0-59 (minute of the hour)";
                }
                return null;
            case DAILY:
                if (intervalValue < 0 || intervalValue > 23) {
                    return "For DAILY interval, intervalValue must be 0-23 (hour of the day)";
                }
                if (timeValue < 0 || timeValue > 59) {
                    return "For DAILY interval, timeValue must be 0-59 (minute of the hour)";
                }
                return null;
            case WEEKLY:
                if (intervalValue < 0 || intervalValue > 6) {
                    return "For WEEKLY interval, intervalValue must be 0-6 (day of week, 0=Sunday)";
                }
                if (timeValue < 0 || timeValue > 1439) {
                    return "For WEEKLY interval, timeValue must be 0-1439 (minute of the day)";
                }
                return null;
            default:
                return "Unknown interval type";
        }
    }

    private ObjectNode actionToJson(ScheduleAction action) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", action.getId());
        node.put("translationKey", action.getTranslationKey());
        node.put("inputType", action.getInputType().name());
        if (action.getInputTranslationKey() != null) {
            node.put("inputTranslationKey", action.getInputTranslationKey());
        }
        return node;
    }

    private ObjectNode scheduleToJson(Schedule schedule) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", schedule.getId());
        node.put("name", schedule.getName());
        node.put("interval", schedule.getInterval().name());
        node.put("intervalValue", schedule.getIntervalValue());
        node.put("timeValue", schedule.getTimeValue());
        node.put("enabled", schedule.isEnabled());
        node.put("lastRun", schedule.getLastRun());
        node.put("description", schedule.getTimingDescription());

        ArrayNode tasksArray = getMapper().createArrayNode();
        for (ScheduleTask task : schedule.getTasks()) {
            tasksArray.add(taskToJson(task));
        }
        node.set("tasks", tasksArray);

        return node;
    }

    private ObjectNode taskToJson(ScheduleTask task) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", task.getId());
        node.put("scheduleId", task.getScheduleId());
        node.put("actionId", task.getActionId());
        node.put("metadata", task.getMetadata());
        node.put("executionOrder", task.getExecutionOrder());
        return node;
    }
}
