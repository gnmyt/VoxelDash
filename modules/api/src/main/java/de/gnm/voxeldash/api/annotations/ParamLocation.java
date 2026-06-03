package de.gnm.voxeldash.api.annotations;

public enum ParamLocation {

    /**
     * Sent in the JSON request body. For {@code GET} routes (which parse their
     * parameters from the query string) the generator automatically treats
     * {@code BODY} fields as query parameters instead.
     */
    BODY,

    /**
     * Sent as a query-string parameter (e.g. {@code ?page=1}).
     */
    QUERY,

    /**
     * Part of the URL path (e.g. the {@code :backupName} in {@code /backups/:backupName}).
     * Path parameters are auto-detected from the route path; use this only to attach
     * a description or type to one of them.
     */
    PATH

}
