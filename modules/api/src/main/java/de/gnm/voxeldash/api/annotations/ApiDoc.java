package de.gnm.voxeldash.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiDoc {

    /**
     * A short, one-line summary of what the endpoint does.
     *
     * @return the summary
     */
    String summary();

    /**
     * A longer description of the endpoint. Supports Markdown/CommonMark.
     *
     * @return the description, or an empty string if none
     */
    String description() default "";

    /**
     * The tag (group) this endpoint belongs to in the rendered docs.
     * When left empty the generator derives it from the router class name
     * (e.g. {@code BackupRouter} becomes {@code Backups}).
     *
     * @return the tag, or an empty string to auto-derive
     */
    String tag() default "";

    /**
     * Whether this endpoint is deprecated.
     *
     * @return {@code true} if deprecated
     */
    boolean deprecated() default false;

}
