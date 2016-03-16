package org.exoplatform.tool;

/*
 * Copyright (C) 2003-${YEAR} eXo Platform SAS.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href=
 *         "http://www.gotoquiz.com/web-coding/programming/java-programming/how-to-extract-titles-from-web-pages-in-java/"
 *         >Source (1)</a><br/>
 *         <a href="http://stackoverflow.com/a/24239268">Source (2)</a>
 */
public class TitleExtractor {
  /*
   * the CASE_INSENSITIVE flag accounts for sites that use uppercase title tags.
   * the DOTALL flag accounts for sites that have line feeds in the title text
   */
  private static final Pattern TITLE_TAG = Pattern.compile("<title>(.*)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /**
   * @param url the HTML page
   * @return title text (null if document isn't HTML or lacks a title tag)
   * @throws IOException
   */
  public static String getPageTitle(String url) throws IOException {
    URL u = new URL(url);
    URLConnection conn = u.openConnection();

    // ContentType is an inner class defined below
    ContentType contentType = getContentTypeHeader(conn);
    if (contentType == null || !contentType.contentType.equals("text/html"))
      return null; // don't continue if not HTML
    else {
      // determine the charset, or use the default
      Charset charset = getCharset(contentType);
      if (charset == null)
        charset = Charset.defaultCharset();

      // read the response body, using BufferedReader for performance
      InputStream in = conn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
      int n, totalRead = 0;
      char[] buf = new char[1024];
      StringBuilder content = new StringBuilder();

      // read until EOF or first 8192 characters
      while (totalRead < 8192 && (n = reader.read(buf, 0, buf.length)) != -1) {
        content.append(buf, 0, n);
        totalRead += n;
      }
      reader.close();

      // extract the title
      Matcher matcher = TITLE_TAG.matcher(content);
      if (matcher.find()) {
        /*
         * replace any occurrences of whitespace (which may include line feeds
         * and other uglies) as well as HTML brackets with a space
         */
        return matcher.group(1).replaceAll("[\\s<>]+", " ").trim();
      } else
        return null;
    }
  }

  /**
   * Loops through response headers until Content-Type is found.
   * 
   * @param conn the url connection to analyze
   * @return ContentType object representing the value of the Content-Type
   *         header
   */
  private static ContentType getContentTypeHeader(URLConnection conn) {
    int i = 0;
    boolean moreHeaders;
    do {
      String headerName = conn.getHeaderFieldKey(i);
      String headerValue = conn.getHeaderField(i);
      if (headerName != null && headerName.equals("Content-Type"))
        return new ContentType(headerValue);

      i++;
      moreHeaders = headerName != null || headerValue != null;
    } while (moreHeaders);

    return null;
  }

  private static Charset getCharset(ContentType contentType) {
    if (contentType != null && contentType.charsetName != null && Charset.isSupported(contentType.charsetName))
      return Charset.forName(contentType.charsetName);
    else
      return null;
  }

  /**
   * Class holds the content type and charset (if present)
   */
  private static final class ContentType {
    private static final Pattern CHARSET_HEADER = Pattern.compile("charset=([-_a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE
                                                    | Pattern.DOTALL);

    private String               contentType;

    private String               charsetName;

    private ContentType(String headerValue) {
      if (headerValue == null)
        throw new IllegalArgumentException("ContentType must be constructed with a not-null headerValue");
      int n = headerValue.indexOf(";");
      if (n != -1) {
        contentType = headerValue.substring(0, n);
        Matcher matcher = CHARSET_HEADER.matcher(headerValue);
        if (matcher.find())
          charsetName = matcher.group(1);
      } else
        contentType = headerValue;
    }
  }
}
