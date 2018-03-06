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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.internal.MockConfigurationSource;

/**
 * A generic JUnit Test used by {@link ExtendedRenderingTestSuite} to parse some passed content and verify it matches some
 * passed expectation. The format of the input/expectation is specified in {@link ExtendedTestDataParser}.
 *
 * @version $Id: 12acffea6e581505f760f5095d2f64f3406c0f7d $
 * @since 3.0RC1
 */
public class ExtendedRenderingTester
{
    /**
     * Symbols to start a special syntax block. For example: <code>${{{regex:...}}}</code> or
     * <code>${{{velocity:...}}}</code>
     */
    private static final String SPECIAL_SYNTAX_START = "${{{";

    /**
     * Symbols to close a special syntax block. For example: <code>${{{regex:...}}}</code> or
     * <code>${{{velocity:...}}}</code>
     */
    private static final String SPECIAL_SYNTAX_END = "}}}";

    private String input;

    private String expected;

    private String parserId;

    private String targetSyntaxId;

    private boolean streaming;

    private List<String> transformations;

    private Map<String, ?> configuration;

    private ComponentManager componentManager;

    public ExtendedRenderingTester(String input, String expected, String parserId, String targetSyntaxId, boolean streaming,
        List<String> transformations, Map<String, ?> configuration, ComponentManager componentManager)
    {
        this.input = input;
        this.expected = expected;
        this.parserId = parserId;
        this.targetSyntaxId = targetSyntaxId;
        this.streaming = streaming;
        this.transformations = transformations;
        this.configuration = configuration;
        this.componentManager = componentManager;
    }

    @Test
    public void execute() throws Throwable
    {
        Map<String, String> originalConfiguration = new HashMap<String, String>();
        if (this.configuration != null) {
            ConfigurationSource configurationSource = getComponentManager().getInstance(ConfigurationSource.class);

            if (configurationSource instanceof MockConfigurationSource) {
                MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                for (Map.Entry<String, ?> entry : this.configuration.entrySet()) {
                    originalConfiguration.put(entry.getKey(),
                        mockConfigurationSource.<String>getProperty(entry.getKey()));
                    mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            runTestInternal();
        } finally {
            // Revert Configuration that have been set
            if (this.configuration != null) {
                ConfigurationSource configurationSource = getComponentManager().getInstance(ConfigurationSource.class);

                if (configurationSource instanceof MockConfigurationSource) {
                    MockConfigurationSource mockConfigurationSource = (MockConfigurationSource) configurationSource;

                    for (Map.Entry<String, String> entry : originalConfiguration.entrySet()) {
                        if (entry.getValue() == null) {
                            mockConfigurationSource.removeProperty(entry.getKey());
                        } else {
                            mockConfigurationSource.setProperty(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }
    }

    private void runTestInternal() throws Throwable
    {
        WikiPrinter printer = new DefaultWikiPrinter();

        StreamParser streamParser = getComponentManager().getInstance(StreamParser.class, this.parserId);

        if (!this.streaming) {
            Parser parser = getComponentManager().getInstance(Parser.class, this.parserId);
            XDOM xdom = parser.parse(new StringReader(this.input));

            if (this.transformations != null) {
                runTransformations(xdom, streamParser);
            }

            BlockRenderer renderer = getComponentManager().getInstance(BlockRenderer.class, this.targetSyntaxId);

            // remove source syntax from XDOM metadata
            if (xdom.getMetaData() != null) {
                Map<String, Object> metadataMap = new HashMap<String, Object>(xdom.getMetaData().getMetaData());
                metadataMap.remove(MetaData.SYNTAX);
                xdom = new XDOM(xdom.getChildren(), new MetaData(metadataMap));
            }

            renderer.render(xdom, printer);
        } else {
            PrintRendererFactory streamRendererFactory =
                getComponentManager().getInstance(PrintRendererFactory.class, this.targetSyntaxId);
            // remove source syntax from begin/endDocument metadata
            WrappingListener listener = new ExtendedSyntaxWrappingListener();
            listener.setWrappedListener(streamRendererFactory.createRenderer(printer));

            streamParser.parse(new StringReader(this.input), listener);
        }

        // Verify the expected result against the result we got.
        assertExpectedResult(this.expected, printer.toString());
    }

    private void runTransformations(XDOM xdom, StreamParser streamParser) throws Throwable
    {
        TransformationContext txContext = new TransformationContext(xdom, streamParser.getSyntax());
        txContext.setTargetSyntax(getRendererSyntax());
        txContext.setId("test");

        if (this.transformations.isEmpty()) {
            TransformationManager transformationManager =
                getComponentManager().getInstance(TransformationManager.class);
            transformationManager.performTransformations(xdom, txContext);
        } else {
            for (String txHint : this.transformations) {
                Transformation transformation = getComponentManager().getInstance(Transformation.class, txHint);
                transformation.transform(xdom, txContext);
            }
        }
    }

    private Syntax getRendererSyntax() throws Exception
    {
        Syntax syntax;
        if (getComponentManager().hasComponent(PrintRendererFactory.class, this.targetSyntaxId)) {
            PrintRendererFactory streamRendererFactory =
                getComponentManager().getInstance(PrintRendererFactory.class, this.targetSyntaxId);
            syntax = streamRendererFactory.getSyntax();
        } else {
            syntax = Syntax.valueOf(this.targetSyntaxId);
        }
        return syntax;
    }

    /**
     * Compare the passed expected string with the passed result. We support regexes for comparison usng the format:
     * ${{{regex:...}}}. For example: <code>
     *  .#-----------------------------------------------------
     *  .expect|event/1.0
     *  .#-----------------------------------------------------
     *  beginDocument
     *  beginMacroMarkerStandalone [useravatar] [username=XWiki.UserNotExisting]
     *  beginGroup [[class]=[xwikirenderingerror]]
     *  onWord [Failed to execute the [useravatar] macro]
     *  endGroup [[class]=[xwikirenderingerror]]
     *  beginGroup [[class]=[xwikirenderingerrordescription hidden]]
     *  onVerbatim [org.xwiki.rendering.macro.MacroExecutionException: User [XWiki.UserNotExisting]${{{regex:.*}}}]
     *  endGroup [[class]=[xwikirenderingerrordescription hidden]]
     *  endMacroMarkerStandalone [useravatar] [username=XWiki.UserNotExisting]
     *  endDocument
     * </code>
     */
    private void assertExpectedResult(String expected, String result)
    {
        StringBuilder builder = new StringBuilder();
        normalizeExpectedValue(builder, expected);

        Pattern pattern = Pattern.compile(builder.toString(), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);
        if (!matcher.matches()) {
            throw new ComparisonFailure("", expected, result);
        }
    }

    private void normalizeExpectedValue(StringBuilder builder, String expected)
    {
        String fullSpecialSyntaxStart = String.format("%sregex:", SPECIAL_SYNTAX_START);
        int pos = expected.indexOf(fullSpecialSyntaxStart);
        if (pos > -1) {
            builder.append(Pattern.quote(expected.substring(0, pos)));
            // Find end of regex definition
            int pos2 = findPositionOfRegexEnd(expected, pos + fullSpecialSyntaxStart.length());
            if (pos2 == -1) {
                throw new RuntimeException("Invalid regex declaration: missing closing part }}}");
            }
            builder.append(expected.substring(pos + fullSpecialSyntaxStart.length(), pos2));
            normalizeExpectedValue(builder, expected.substring(pos2 + 3));
        } else {
            builder.append(Pattern.quote(expected));
        }
    }

    private int findPositionOfRegexEnd(String expected, int pos)
    {
        int result = expected.indexOf(SPECIAL_SYNTAX_END, pos);
        // Verify the first char of the SPECIAL_SYNTAX_END is not escaped
        if (result > -1 && expected.charAt(result - 1) == '\\') {
            result = findPositionOfRegexEnd(expected, result + 1);
        }
        return result;
    }

    public ComponentManager getComponentManager() throws Exception
    {
        return this.componentManager;
    }
}
