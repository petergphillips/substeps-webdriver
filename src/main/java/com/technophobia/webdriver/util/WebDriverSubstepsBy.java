/*
 *	Copyright Technophobia Ltd 2012
 *
 *   This file is part of Substeps.
 *
 *    Substeps is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Substeps is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with Substeps.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.technophobia.webdriver.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.technophobia.substeps.step.StepImplementationUtils;

/**
 * @author imoore
 */
public abstract class WebDriverSubstepsBy {

    static final Logger logger = LoggerFactory.getLogger(WebDriverSubstepsBy.class);


    public static ByIdAndText ByIdAndText(final String id, final String text) {
        return new ByIdAndText(id, text);
    }


    public static ByIdAndText ByIdAndCaseSensitiveText(final String id, final String text) {
        return new ByIdAndText(id, text, true);
    }


    public static ByTagAndAttributes ByTagAndAttributes(final String tagName,
                                                        final Map<String, String> requiredAttributes) {
        return new ByTagAndAttributes(tagName, requiredAttributes);
    }


    public static ByTagAndAttributes ByTagAndAttributes(final String tagName, final String attributeString) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributes(tagName, expectedAttributes);
    }


    public static ByTagAndAttributes NthByTagAndAttributes(final String tagName, final String attributeString,
                                                           final int nth) {

        final Map<String, String> expectedAttributes = StepImplementationUtils.convertToMap(attributeString);

        return new ByTagAndAttributes(tagName, expectedAttributes, nth);
    }


    public static ByCurrentWebElement ByCurrentWebElement(final WebElement elem) {
        return new ByCurrentWebElement(elem);
    }


    public static ByTagAndWithText ByTagAndWithText(final String tag, final String text) {
        return new ByTagAndWithText(tag, text);
    }


    public static ByTagAndWithText ByTagContainingText(final String tag, final String text) {
        return new ByTagAndContainingText(tag, text);
    }


    public static ByTagAndWithText ByTagStartingWithText(final String tag, final String text) {
        return new ByTagAndStartingWithText(tag, text);
    }


    public static ByIdContainingText ByIdContainingText(final String id, final String text) {
        return new ByIdContainingText(id, text);
    }


    public static BySomethingContainingText ByXpathContainingText(final String xpath, final String text) {
        return new BySomethingContainingText(By.xpath(xpath), text);
    }


    static abstract class BaseBy extends By {

        @Override
        public final List<WebElement> findElements(final SearchContext context) {
            List<WebElement> matchingElems = findElementsBy(context);

            if (matchingElems == null) {
                matchingElems = Collections.emptyList();
            }

            return matchingElems;
        }


        public abstract List<WebElement> findElementsBy(final SearchContext context);
    }

    static abstract class XPathBy extends BaseBy {

        @Override
        public List<WebElement> findElementsBy(SearchContext context) {

            final StringBuilder xpathBuilder = new StringBuilder();

            buildXPath(xpathBuilder);

            return context.findElements(By.xpath(xpathBuilder.toString()));
        }

        protected abstract void buildXPath(StringBuilder xpathBuilder);
    }

    static class ByTagAndAttributes extends XPathBy {

        private final String tagName;
        private final Map<String, String> requiredAttributes;
        private final int minimumExpected;


        ByTagAndAttributes(final String tagName, final Map<String, String> requiredAttributes) {
            this.tagName = tagName;
            this.requiredAttributes = requiredAttributes;
            this.minimumExpected = 1;
        }


        ByTagAndAttributes(final String tagName, final Map<String, String> requiredAttributes, final int nth) {
            this.tagName = tagName;
            this.requiredAttributes = requiredAttributes;
            this.minimumExpected = nth;
        }


        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(tagName);

            if (!requiredAttributes.isEmpty()) {
                xpathBuilder.append("[");

                boolean firstOne = true;

                for (Map.Entry<String, String> requiredAttribute : requiredAttributes.entrySet()) {

                    if (!firstOne) {
                        xpathBuilder.append(" and ");
                    }

                    xpathBuilder.append("@").append(requiredAttribute.getKey()).append(" = '").append(requiredAttribute.getValue()).append("'");

                    firstOne = false;
                }

                xpathBuilder.append("]");
            }

        }

        @Override
        public List<WebElement> findElementsBy(final SearchContext searchContext) {
            final List<WebElement> matchingElems = super.findElementsBy(searchContext);

            if (matchingElems != null && matchingElems.size() < this.minimumExpected) {
                logger.info("expecting at least " + this.minimumExpected + " matching elems, found only "
                        + matchingElems.size() + " this time around");
                // we haven't found enough, clear out
                return null;
            }

            return matchingElems;
        }
    }

    /**
     * A By for use with the current web element, to be chained with other Bys
     */
    static class ByCurrentWebElement extends BaseBy {

        private final WebElement currentElement;


        public ByCurrentWebElement(final WebElement elem) {
            this.currentElement = elem;
        }


        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            final List<WebElement> matchingElems = new ArrayList<WebElement>();
            matchingElems.add(this.currentElement);

            return matchingElems;
        }
    }

    static class ByTagAndWithText extends XPathBy {

        protected final String tag;
        protected final String text;


        ByTagAndWithText(final String tag, final String text) {
            this.tag = tag;
            this.text = text;
        }


        @Override
        protected void buildXPath(StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[").append(equalsIgnoringCaseXPath("text()", "'" + this.text.toLowerCase() + "'")).append("]");
        }

    }

    static class ByTagAndContainingText extends ByTagAndWithText {

        ByTagAndContainingText(String tag, String text) {
            super(tag, text);
        }

        @Override
        protected void buildXPath(StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[contains(text(), '").append(this.text).append("')]");
        }
    }

    static class ByTagAndStartingWithText extends ByTagAndWithText {

        ByTagAndStartingWithText(String tag, String text) {
            super(tag, text);
        }

        @Override
        protected void buildXPath(StringBuilder xpathBuilder) {
            xpathBuilder.append(".//").append(this.tag).append("[starts-with(text(), '").append(this.text).append("')]");
        }
    }

    static class BySomethingContainingText extends BaseBy {

        protected final String text;
        protected final By by;


        BySomethingContainingText(final By by, final String text) {
            this.by = by;
            this.text = text;
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.openqa.selenium.By#findElements(org.openqa.selenium.SearchContext
         * )
         */
        @Override
        public List<WebElement> findElementsBy(final SearchContext context) {

            List<WebElement> matchingElems = null;

            final List<WebElement> elems = context.findElements(this.by);
            if (elems != null) {
                for (final WebElement e : elems) {

                    if (e.getText() != null && e.getText().contains(this.text)) {

                        if (matchingElems == null) {
                            matchingElems = new ArrayList<WebElement>();
                        }
                        matchingElems.add(e);
                    }
                }
            }

            return matchingElems;
        }
    }

    static class ByIdContainingText extends XPathBy {

        protected final String text;
        protected final String id;


        ByIdContainingText(final String id, final String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {

            xpathBuilder.append(".//*[@id='")
                    .append(this.id)
                    .append("' and contains(text(), '")
                    .append(this.text)
                    .append("')]");

        }
    }

    static class ByIdAndText extends XPathBy {

        protected final String text;
        protected final String id;
        protected final boolean caseSensitive;


        ByIdAndText(final String id, final String text) {
            this(id, text, false);
        }


        ByIdAndText(final String id, final String text, final boolean caseSensitive) {
            this.id = id;
            this.text = text;
            this.caseSensitive = caseSensitive;
        }


        @Override
        protected void buildXPath(final StringBuilder xpathBuilder) {

            xpathBuilder.append(".//*[@id='").append(this.id).append("' and ");

            if (caseSensitive) {
                xpathBuilder.append("text()='").append(this.text).append("'");
            } else {
                xpathBuilder.append(equalsIgnoringCaseXPath("text()", "'" + this.text.toLowerCase() + "'"));
            }

            xpathBuilder.append("]");
        }
    }

    private static String equalsIgnoringCaseXPath(String str1, String str2) {
        return new StringBuilder().append("translate(")
                .append(str1)
                .append(", 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=")
                .append(str2).toString();
    }
}
