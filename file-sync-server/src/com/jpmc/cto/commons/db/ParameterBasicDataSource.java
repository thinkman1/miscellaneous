package com.jpmc.cto.commons.db;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * Datasource that allows the setting of properties
 *
 * @author Jim Kerwood
 */
public class ParameterBasicDataSource extends BasicDataSource {
    /**
     * Sets properties on the datasource.
     * @param p the properties to set on the connection
     */
    public void setProperties(final Properties p) {
        Enumeration e = p.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = (String) p.get(key);
            this.addConnectionProperty(key, value);
        }
    }
}
