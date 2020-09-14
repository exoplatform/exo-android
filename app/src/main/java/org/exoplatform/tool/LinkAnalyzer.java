package org.exoplatform.tool;

/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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
 *
 */

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exoplatform.BuildConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes a link with Jsoup, and return a set of metadata.
 *
 * @author paristote on 4/11/16.
 */
public class LinkAnalyzer {

    public static class Metadata {
        public String title;
        public String description;
        public List<String> imageUrlList;
    }

    public static
    @Nullable
    Metadata getMetadata(@NonNull String url) throws MalformedURLException {
        new URL(url); // Just checking the given url
        Metadata metadata = null;
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            // We get better content by pretending to be a desktop browser...
            metadata = new Metadata();
            metadata.title = getTitle(doc);
            String desc = getDescription(doc);
            if ("".equals(desc)) {
                metadata.description = extractExcerptFromContent(doc);
            } else {
                metadata.description = desc;
            }
            List<String> imgs = getImages(doc);
            if (imgs.isEmpty()) {
                metadata.imageUrlList = extractImagesFromContent(doc);
            } else {
                metadata.imageUrlList = imgs;
            }
        } catch (IOException e) {
            Log.w("LinkAnalyzer", "Cannot retrieve document at url " + url, e);
        }

        return metadata;
    }

    /**
     * Extract the title, from og:title or the title tag
     *
     * @param jsoupDoc the Jsoup Document
     * @return the title or an empty string
     */
    private static String getTitle(@NonNull Document jsoupDoc) {
        String title;
        Elements metaOgTitle = jsoupDoc.select("meta[property=og:title]");
        if (!metaOgTitle.isEmpty())
            title = metaOgTitle.attr("content");
        else
            title = jsoupDoc.title();
        return title;
    }

    /**
     * Extract the og:description meta element of the page
     *
     * @param jsoupDoc the Jsoup Document
     * @return the description or an empty string
     */
    private static String getDescription(@NonNull Document jsoupDoc) {
        String desc = "";
        Elements metaOgDesc = jsoupDoc.select("meta[property=og:description]");
        if (metaOgDesc.isEmpty())
            metaOgDesc = jsoupDoc.select("meta[name=description]");
        if (!metaOgDesc.isEmpty())
            desc = metaOgDesc.attr("content");
        return desc;
    }

    /**
     * Get any og:image meta element and return the list of URLs
     *
     * @param jsoupDoc the Jsoup Document
     * @return a list of URL strings
     */
    private static List<String> getImages(@NonNull Document jsoupDoc) {
        List<String> urls = new ArrayList<>();
        Elements metaOgImage = jsoupDoc.select("meta[property=og:image]");
        for (int i = 0; i < metaOgImage.size(); i++) {
            Element imgElement = metaOgImage.get(i);
            if (imgElement.hasAttr("content") && imgElement.attr("content").startsWith("http"))
                urls.add(imgElement.attr("content"));
        }
        return urls;
    }

    /**
     * Find the global container element, based on typical class or tag names
     *
     * @param jsoupDoc the Jsoup Document
     * @return a container Element, or null
     */
    private static Element getContentElement(@NonNull Document jsoupDoc) {
        Elements container = jsoupDoc.select(".container");
        if (container.isEmpty())
            container = jsoupDoc.select(".content");
        if (container.isEmpty())
            container = jsoupDoc.select(".article-page");
        if (container.isEmpty())
            container = jsoupDoc.select(".article");
        if (container.isEmpty())
            container = jsoupDoc.select(".entry-content");
        if (container.isEmpty())
            container = jsoupDoc.select(".site-content");
        if (container.isEmpty())
            container = jsoupDoc.select("article");
        if (container.isEmpty())
            container = jsoupDoc.select("main");

        return container.first();
    }

    /**
     * Extracts URLs of images contained in the document
     *
     * @param jsoupDoc the Jsoup Document
     * @return a list of image source URLs (can be empty)
     */
    private static List<String> extractImagesFromContent(@NonNull Document jsoupDoc) {
        List<String> urls = new ArrayList<>();
        Element container = getContentElement(jsoupDoc);
        if (container != null) {
            // List of images with absolute src url
            Elements images = container.select("img[src^=http]");
            for (int i = 0; i < images.size(); i++) {
                urls.add(images.get(i).attr("src"));
            }
        }
        return urls;
    }

    /**
     * Extracts the 1st paragraph of text from the document
     *
     * @param jsoupDoc the Jsoup Document
     * @return A text clipped at 250 chars, or an empty string
     */
    private static String extractExcerptFromContent(@NonNull Document jsoupDoc) {
        String excerpt = "";
        Element container = getContentElement(jsoupDoc);
        if (container != null) {
            Elements paragraphs = container.select("p");
//            TODO check if other elements often have text
//            if (paragraphs.isEmpty())
//                paragraphs = container.select("?");

            if (!paragraphs.isEmpty()) {
                for (int i = 0; i < paragraphs.size(); i++) {
                    if (paragraphs.get(i).hasText()) {
                        String text = paragraphs.get(i).text();
                        excerpt = text.length() > 250 ? text.substring(0, 250) + "..." : text;
                        break;
                    }
                }
            }
        }
        return excerpt;
    }
}
