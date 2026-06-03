package de.gnm.voxeldash.api.annotations;

public enum FieldType {

    /**
     * A text value (JSON string).
     */
    STRING("string"),

    /**
     * A whole number (JSON integer).
     */
    INTEGER("integer"),

    /**
     * A floating point number (JSON number).
     */
    NUMBER("number"),

    /**
     * A boolean ({@code true}/{@code false}).
     */
    BOOLEAN("boolean"),

    /**
     * A nested JSON object.
     */
    OBJECT("object"),

    /**
     * A JSON array.
     */
    ARRAY("array");

    private final String schemaType;

    FieldType(String schemaType) {
        this.schemaType = schemaType;
    }

    /**
     * Gets the OpenAPI/JSON-schema type name for this field type.
     *
     * @return the schema type name (e.g. {@code "string"})
     */
    public String getSchemaType() {
        return schemaType;
    }
}
