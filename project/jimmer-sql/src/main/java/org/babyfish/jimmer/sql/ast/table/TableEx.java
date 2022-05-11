package org.babyfish.jimmer.sql.ast.table;

/**
 * Difference between TableEx and Table
 *
 * <table>
 *     <tr>
 *         <td></td>
 *         <td>
 *             <b>TableEx</b>
 *         </td>
 *         <td>
 *             <b>Table</b>
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             Used by
 *         </td>
 *         <td>
 *             subQuery, update and update.
 *         </td>
 *         <td>
 *             top-level query
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             Effect
 *         </td>
 *         <td>
 *             Generated code can be used to join any associations
 *         </td>
 *         <td>
 *             Generated code can only be used to join non-collection associations
 *         </td>
 *     </tr>
 * </table>
 */
public interface TableEx<E> extends Table<E> { }
