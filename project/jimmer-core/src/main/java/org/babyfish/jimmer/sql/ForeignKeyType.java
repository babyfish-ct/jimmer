package org.babyfish.jimmer.sql;

public enum ForeignKeyType {

    /**
     * Use the global configuration to decide whether a foreign key is real or fake.
     *
     * <p>By default, `AUTO` and `REAL` are equivalent.</p>
     *
     * <ul>
     *     <li>
     *         If spring boot is used, foreign keys will be considered as fake
     *          if `jimmer.is-foreign-key-enabled-by-default` is set to false
     *     </li>
     *     <li>
     *         Otherwise, foreign keys will be considered as fake if developer create sqlClient like this
     *         <pre>{@code
     *             JSqlClient sqlClient = JSqlClient
     *                  .newBuilder()
     *                  .setForeignKeyEnabledByDefault(false)
     *                  .build()
     *         }</pre>
     *     </li>
     * </ul>
     */
    AUTO,

    REAL,
    FAKE
}
