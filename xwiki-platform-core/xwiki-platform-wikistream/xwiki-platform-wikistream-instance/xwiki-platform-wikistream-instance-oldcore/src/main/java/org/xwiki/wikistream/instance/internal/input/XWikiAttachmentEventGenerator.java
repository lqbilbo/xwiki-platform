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
package org.xwiki.wikistream.instance.internal.input;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiAttachmentFilter;
import org.xwiki.wikistream.instance.input.EntityEventGenerator;
import org.xwiki.wikistream.instance.internal.XWikiAttachmentFilter;
import org.xwiki.wikistream.instance.internal.XWikiAttachmentProperties;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiAttachmentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
// TODO: add support for real revision events (instead of the jrcs archive )
public class XWikiAttachmentEventGenerator extends
    AbstractBeanEntityEventGenerator<XWikiAttachment, XWikiAttachmentFilter, XWikiAttachmentProperties>
{
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiAttachment.class, XWikiAttachmentProperties.class);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public void write(XWikiAttachment attachment, Object filter, XWikiAttachmentFilter attachmentFilter,
        XWikiAttachmentProperties properties) throws WikiStreamException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // > WikiAttachment

        attachmentFilter.beginWikiAttachment(attachment.getFilename(), FilterEventParameters.EMPTY);

        // > WikiAttachmentRevision

        FilterEventParameters parameters = new FilterEventParameters();

        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, attachment.getAuthor());
        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, attachment.getComment());
        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, attachment.getDate());

        if (properties.isWithWikiAttachmentContent()) {
            try {
                parameters.put(WikiAttachmentFilter.PARAMETER_CONTENT, attachment.getContentInputStream(xcontext));
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to get content for attachment [" + attachment.getReference()
                    + "]", e);
            }
        }

        if (properties.isWithWikiAttachmentRevisions()) {
            try {
                // We need to make sure content is loaded
                XWikiAttachmentArchive archive;
                archive = attachment.loadArchive(xcontext);
                if (archive != null) {
                    parameters.put(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS, new String(archive.getArchive()));
                }
            } catch (XWikiException e) {
                this.logger.error("Attachment [{}] has malformed history", attachment.getReference(), e);
            }
        }

        attachmentFilter.beginWikiAttachmentRevision(attachment.getVersion(), parameters);

        // < WikiAttachmentRevision

        attachmentFilter.endWikiAttachmentRevision(attachment.getVersion(), parameters);

        // < WikiAttachment

        attachmentFilter.endWikiAttachment(attachment.getFilename(), FilterEventParameters.EMPTY);
    }
}
