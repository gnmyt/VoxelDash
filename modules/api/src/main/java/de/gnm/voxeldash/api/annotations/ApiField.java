package de.gnm.voxeldash.api.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ApiFields.class)
public @interface ApiField {

    /**
     * The name of the field as sent by the client.
     *
     * @return the field name
     */
    String name();

    /**
     * The JSON type of the field.
     *
     * @return the field type, defaults to {@link FieldType#STRING}
     */
    FieldType type() default FieldType.STRING;

    /**
     * Whether the field is required.
     *
     * @return {@code true} if required, defaults to {@code true}
     */
    boolean required() default true;

    /**
     * A human-readable description of the field.
     *
     * @return the description, or an empty string if none
     */
    String description() default "";

    /**
     * Where the field is supplied by the client.
     *
     * @return the location, defaults to {@link ParamLocation#BODY}
     */
    ParamLocation in() default ParamLocation.BODY;

}
