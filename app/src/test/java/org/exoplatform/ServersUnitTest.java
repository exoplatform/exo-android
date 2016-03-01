package org.exoplatform;

import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by paristote on 3/3/16.
 */
public class ServersUnitTest {

    /*
        TEST MODEL Server
     */

    @Test
    public void createServer1() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        Server srv = new Server(new URL(url));

        assertEquals(new URL(url), srv.getUrl());
        assertEquals(-1L, srv.getLastVisited().longValue());
        assertEquals("", srv.getLastLogin());
    }

    @Test
    public void createServer2() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        long lastVisit = System.currentTimeMillis();
        Server srv = new Server(new URL(url), lastVisit);

        assertEquals(new URL(url), srv.getUrl());
        assertEquals(lastVisit, srv.getLastVisited().longValue());
        assertEquals("", srv.getLastLogin());
    }

    @Test
    public void createServer3() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        long lastVisit = System.currentTimeMillis();
        String lastUser = "john";
        Server srv = new Server(new URL(url), lastVisit, lastUser);

        assertEquals(new URL(url), srv.getUrl());
        assertEquals(lastVisit, srv.getLastVisited().longValue());
        assertEquals(lastUser, srv.getLastLogin());
    }

    @Test
    public void createServer4() throws MalformedURLException {
        String url = "https://community.exoplatform.com/some/path";
        Server srv = new Server(new URL(url));

        String formattedUrl = "https://community.exoplatform.com";
        assertEquals(new URL(formattedUrl), srv.getUrl());
    }

    @Test
    public void createServer5() throws MalformedURLException {
        String url = "https://community.exoplatform.com:8080";
        Server srv = new Server(new URL(url));

        assertEquals(new URL(url), srv.getUrl());
    }

    @Test
    public void editServer() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        long lastVisit = System.currentTimeMillis();
        String lastUser = "john";
        Server srv = new Server(new URL(url), lastVisit, lastUser);

        String newUser = "mary";
        long newVisit = lastVisit + 1000;
        URL newUrl = new URL("https://www.exoplatform.com");
        srv.setLastLogin(newUser);
        srv.setLastVisited(newVisit);
        srv.setUrl(newUrl);

        assertEquals(newUrl, srv.getUrl());
        assertEquals(newVisit, srv.getLastVisited().longValue());
        assertEquals(newUser, srv.getLastLogin());
    }

    @Test
    public void formatServerUrl() throws MalformedURLException {
        // no change
        URL testUrl = new URL("https://community.exoplatform.com");
        assertEquals("https://community.exoplatform.com", Server.format(testUrl));

        // remove path
        testUrl = new URL("https://community.exoplatform.com/some/path");
        assertEquals("https://community.exoplatform.com", Server.format(testUrl));

        // keep port number
        testUrl = new URL("https://community.exoplatform.com:8080");
        assertEquals("https://community.exoplatform.com:8080", Server.format(testUrl));

        // with http
        testUrl = new URL("http://community.exoplatform.com");
        assertEquals("http://community.exoplatform.com", Server.format(testUrl));
    }

    @Test
    public void getShortUrl() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        Server srv = new Server(new URL(url));

        assertEquals("community.exoplatform.com", srv.getShortUrl());
    }

    @Test
    public void isExoTribe1() throws MalformedURLException {
        String url = "https://community.exoplatform.com";
        Server srv = new Server(new URL(url));

        assertTrue(srv.isExoTribe());
    }

    @Test
    public void isExoTribe2() throws MalformedURLException {
        String url = "http://community.exoplatform.com";
        Server srv = new Server(new URL(url));

        assertTrue(srv.isExoTribe());
    }

    @Test
    public void isNotExoTribe() throws MalformedURLException {
        String url = "https://www.exoplatform.com";
        Server srv = new Server(new URL(url));

        assertFalse(srv.isExoTribe());
    }


    /*
        TEST CLASS ServerUtils
     */

    @Test
    public void convertVersionFromString() {

        Double v4 = ServerUtils.convertVersionFromString("4");
        Double v43 = ServerUtils.convertVersionFromString("4.3");
        Double v431 = ServerUtils.convertVersionFromString("4.3.1");

        assertEquals(4., v4.doubleValue(), 0.);
        assertEquals(4.3, v43.doubleValue(), 0.);
        assertEquals(4.3, v431.doubleValue(), 0.);

        try {
            ServerUtils.convertVersionFromString("abc");
            fail("Should have raised a NumberFormatException");
        } catch (Exception e) {
            assertTrue(e instanceof NumberFormatException);
        }

        try {
            ServerUtils.convertVersionFromString(null);
            fail("Should have raised an IllegalArgumentException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void readStream() throws IOException {
        String str = "lorem ipsum\n" +
                    "dolor amet\n" +
                    "=====---=====\n" +
                    "Hello,World!";
        InputStream stream = new ByteArrayInputStream(str.getBytes("UTF-8"));
        String convertedStr = ServerUtils.readStream(stream);

        assertEquals(str, convertedStr);
    }

}
