/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.springframework.security.core.Authentication;

/**
 * Filter function that returns true if a certain object can or cannot be showed to the current user
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class InMemorySecurityFilter extends InternalVolatileFunction {

    ResourceAccessManager resourceAccesssManager;

    Authentication user;

    /**
     * Returns a filter that will check if the object passed to it can be accessed by the user
     * 
     * @param resourceAccesssManager
     * @param user
     * @return
     */
    public static Filter buildUserAccessFilter(ResourceAccessManager resourceAccesssManager,
            Authentication user) {
        org.opengis.filter.expression.Function visible = new InMemorySecurityFilter(
                resourceAccesssManager, user);

        FilterFactory factory = Predicates.factory;

        // create a filter combined with the security credentials check
        Filter filter = factory.equals(factory.literal(Boolean.TRUE), visible);

        return filter;
    }

    public InMemorySecurityFilter(ResourceAccessManager resourceAccesssManager, Authentication user) {
        super();
        this.resourceAccesssManager = resourceAccesssManager;
        this.user = user;
    }

    private Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    private SecureCatalogImpl getSecurityWrapper() {
        return GeoServerExtensions.bean(SecureCatalogImpl.class);
    }

    @Override
    public Boolean evaluate(Object object) {
        CatalogInfo info = (CatalogInfo) object;
        if (info instanceof NamespaceInfo) {
            info = getCatalog().getWorkspaceByName(((NamespaceInfo) info).getPrefix());
        }
        WrapperPolicy policy = getSecurityWrapper().buildWrapperPolicy(resourceAccesssManager,
                user, info);
        AccessLevel accessLevel = policy.getAccessLevel();
        boolean visible = !AccessLevel.HIDDEN.equals(accessLevel);
        return Boolean.valueOf(visible);
    }

}
