package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;

/**
 * The user's commitment to ensure that the filtered fields of two associated objects are the same.
 *
 * Let's look at an example with the following defined entities:
 * <pre><code>
 *  &#64;MappedSupperclass
 *  public interface TenantAware {
 *      String tenant()
 *  }
 * </code></pre>
 *
 * <pre><code>
 *  &#64;Entity
 *  public interface Department extends TenantAware {
 *      ...
 *  }
 * </code></pre>
 *
 * <pre><code>
 *  &#64;Entity
 *  public interface Employee extends TenantAware {
 *      &#64;ManyToOne
 *      Department department(); // Nonnull many-to-one association
 *      ...
 *  }
 * </code></pre>
 *
 * Here both Department and Employee support multi-tenancy, with a defined filter:
 * <pre><code>
 *  &#64;Entity
 *  public interface Employee extends AssociationIntegrityAssuranceFilter&lt;TenantAwareProps&gt; {
 *      &#64;Override
 *      public void filter(FilterArgs&lt;P&gt; args) {
 *          String tenant = ...get tenant from HTTP header...
 *          if (tenant != null && !tenant.isEmpty()) {
 *              args.where(args.getTable().tenant().eq(tenant));
 *          }
 *      }
 *      ...
 *  }
 * </code></pre>
 *
 * This filter implements the `AssociationIntegrityAssuranceFilter` interface.
 * This indicates the user's commitment that associated `Department` and `Employee` objects must belong to
 * the same tenant, `Department` and `Employee` objects belonging to different tenants will never be associated.
 * If the many-to-one association `Employee.department` could not be null without the filter,
 * then with this filter applied to both `Department` and `Employee` sides, this association also cannot be null.
 *
 * @param <P> Filtered type, it is often a type decorated by &#64;{@link org.babyfish.jimmer.sql.MappedSuperclass}.
 */
public interface AssociationIntegrityAssuranceFilter<P extends Props> extends Filter<P> {
}
