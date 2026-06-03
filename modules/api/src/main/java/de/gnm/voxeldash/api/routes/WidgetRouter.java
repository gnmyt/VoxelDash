package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.controller.WidgetRegistry;
import de.gnm.voxeldash.api.entities.widget.Widget;
import de.gnm.voxeldash.api.entities.widget.WidgetDataPoint;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.http.Response;

import java.util.List;
import java.util.Map;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;

public class WidgetRouter extends BaseRoute {

    @ApiDoc(summary = "List widgets", description = "Returns all registered widgets without their data points.", tag = "Widgets")
    @AuthenticatedRoute
    @Path("/widgets")
    @Method(GET)
    public Response listWidgets() {
        WidgetRegistry registry = getLoader().getWidgetRegistry();

        ArrayNode widgetsArray = getMapper().createArrayNode();
        for (Widget widget : registry.getAllWidgets()) {
            widgetsArray.add(widgetToJson(widget, false));
        }

        return new JSONResponse().add("widgets", widgetsArray);
    }

    @ApiDoc(summary = "Get a widget", description = "Returns a single widget including its data points and metadata.", tag = "Widgets")
    @ApiField(name = "id", in = ParamLocation.PATH, description = "ID of the widget to retrieve")
    @AuthenticatedRoute
    @Path("/widgets/:id")
    @Method(GET)
    public Response getWidget(RawRequest request) {
        String id = request.getParameter("id");

        WidgetRegistry registry = getLoader().getWidgetRegistry();
        Widget widget = registry.getWidget(id);

        if (widget == null) {
            return new JSONResponse().error("Widget not found", 404);
        }

        return new JSONResponse().add("widget", widgetToJson(widget, true));
    }

    @ApiDoc(summary = "Get all widget data", description = "Returns all registered widgets including their data points and metadata.", tag = "Widgets")
    @AuthenticatedRoute
    @Path("/widgets/data")
    @Method(GET)
    public Response getAllWidgetData() {
        WidgetRegistry registry = getLoader().getWidgetRegistry();

        ArrayNode widgetsArray = getMapper().createArrayNode();
        for (Widget widget : registry.getAllWidgets()) {
            widgetsArray.add(widgetToJson(widget, true));
        }

        return new JSONResponse().add("widgets", widgetsArray);
    }

    private ObjectNode widgetToJson(Widget widget, boolean includeData) {
        ObjectNode node = getMapper().createObjectNode();
        node.put("id", widget.getId());
        node.put("translationKey", widget.getTranslationKey());
        node.put("type", widget.getType().name());
        node.put("color", widget.getColor());

        if (widget.getUnit() != null) {
            node.put("unit", widget.getUnit());
        }

        ObjectNode sizeNode = getMapper().createObjectNode();
        sizeNode.put("width", widget.getDefaultSize().getWidth());
        sizeNode.put("height", widget.getDefaultSize().getHeight());
        sizeNode.put("minWidth", widget.getDefaultSize().getMinWidth());
        sizeNode.put("minHeight", widget.getDefaultSize().getMinHeight());
        node.set("defaultSize", sizeNode);

        if (includeData) {
            List<WidgetDataPoint> dataPoints = widget.getData();
            if (dataPoints != null && !dataPoints.isEmpty()) {
                ArrayNode dataArray = getMapper().createArrayNode();
                for (WidgetDataPoint point : dataPoints) {
                    ObjectNode pointNode = getMapper().createObjectNode();
                    pointNode.put("timestamp", point.getTimestamp());
                    pointNode.put("label", point.getLabel());
                    pointNode.put("value", point.getValue());
                    dataArray.add(pointNode);
                }
                node.set("data", dataArray);
            }

            Map<String, Object> metadata = widget.getMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                ObjectNode metaNode = getMapper().createObjectNode();
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        metaNode.put(entry.getKey(), (String) value);
                    } else if (value instanceof Number) {
                        metaNode.put(entry.getKey(), ((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        metaNode.put(entry.getKey(), (Boolean) value);
                    } else if (value != null) {
                        metaNode.put(entry.getKey(), value.toString());
                    }
                }
                node.set("metadata", metaNode);
            }
        }

        return node;
    }
}
