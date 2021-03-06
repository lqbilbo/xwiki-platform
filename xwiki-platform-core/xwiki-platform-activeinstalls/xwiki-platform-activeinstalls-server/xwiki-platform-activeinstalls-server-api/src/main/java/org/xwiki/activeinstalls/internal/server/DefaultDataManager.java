/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.activeinstalls.internal.server;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xwiki.activeinstalls.internal.JestClientManager;
import org.xwiki.activeinstalls.server.DataManager;
import org.xwiki.component.annotation.Component;

import io.searchbox.client.JestResult;
import io.searchbox.core.Count;

/**
 * Get stored ping data from a remote Elastic Search instance.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultDataManager implements DataManager
{
    /**
     * Formatter to format dates in a standard format.
     */
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    @Inject
    private JestClientManager jestClientManager;

    @Override
    public long getTotalInstalls() throws Exception
    {
        return executeCount("{\"match_all\" : { }}");
    }

    @Override
    public long getActiveInstalls(int days) throws Exception
    {
        // Compute current date - N days
        DateTime dt = new DateTime();
        dt = dt.plusDays(-days);

        // Serialize the new date
        String serializedDate = DATE_FORMATTER.print(dt);

        String query = "{\n"
            + "  \"constant_score\" : {\n"
            + "    \"filter\" : {\n"
            + "      \"numeric_range\" : {\n"
            + "        \"date\" : {\n"
            + "          \"gte\" : \"" + serializedDate + "\"\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

        return executeCount(query);
    }

    private long executeCount(String query) throws Exception
    {
        Count count = new Count.Builder(query)
            .addIndex("installs")
            .addType("install")
            .build();
        JestResult result = this.jestClientManager.getClient().execute(count);
        return ((Double) result.getValue("count")).longValue();
    }
}
