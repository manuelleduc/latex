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
package org.xwiki.contrib.latex.internal;

import org.junit.runner.RunWith;
import org.xwiki.rendering.test.cts.CompatibilityTestSuite;
import org.xwiki.rendering.test.cts.Initialized;
import org.xwiki.rendering.test.cts.Syntax;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Run all CTS tests for the LaTeX syntax.
 *
 * @version $Id$
 * @since 1.0
 */
@RunWith(CompatibilityTestSuite.class)
@Syntax("latex/1.0")
@AllComponents
public class LaTeXCompatibilityTest
{
    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        MockSetup.setUp(componentManager);
    }
}
