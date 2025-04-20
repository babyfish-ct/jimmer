package org.babyfish.jimmer.sql.runtime;

public enum DatabaseValidationMode {

    /**
     * Default option, no automatic database validation is required.
     *
     * <p>You need this configuration in the following two cases:</p>
     * <ul>
     *     <li>You do not want to validate the database.</li>
     *     <li>You want to manually validate the database for specific purposes,
     *     such as precisely controlling the timing of validation.
     *     <i>In this case, you need to manually call the `validateDatabase`
     *     method of the `sqlClient` object.</i>
     *     </li>
     * </ul>
     */
    NONE,

    /**
     * Automatically validate the database,
     * and if there are validation errors, throw an exception.
     */
    WARNING,

    /**
     * Automatically validate the database,
     * and if there are validation errors, print a warning.
     */
    ERROR
}
