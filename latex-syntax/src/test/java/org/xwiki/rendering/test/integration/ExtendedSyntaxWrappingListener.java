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
package org.xwiki.rendering.test.integration;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.WrappingListener;

/**
 * Filter {@link MetaData} source element from begin/endDocument events.
 *
 * @version $Id: 3479722a578c3b6a79753408f9d23e1bfebaf161 $
 * @since 3.0RC1
 */
public class ExtendedSyntaxWrappingListener extends WrappingListener
{
    @Override
    public void beginDocument(MetaData metadata)
    {
        Map<String, Object> metadataMap = new HashMap<String, Object>(metadata.getMetaData());
        metadataMap.remove(MetaData.SYNTAX);

        super.beginDocument(new MetaData(metadataMap));
    }

    @Override
    public void endDocument(MetaData metadata)
    {
        Map<String, Object> metadataMap = new HashMap<String, Object>(metadata.getMetaData());
        metadataMap.remove(MetaData.SYNTAX);

        super.endDocument(new MetaData(metadataMap));
    }
}
