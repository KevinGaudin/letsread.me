/**
 * Copyright 2011, 2012 Kevin Gaudin
 *
 * This file is part of letsread.me.
 *
 * letsread.me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * letsread.me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with letsread.me.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers;

import helpers.BookHelper;
import helpers.SampleFeedsHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Feed;
import models.User;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;

import org.w3c.dom.Document;

import play.Logger;
import play.cache.Cache;
import play.data.validation.Required;
import play.libs.F.Promise;
import play.libs.OpenID;
import play.libs.OpenID.UserInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http.Header;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * The application main actions.
 */
public class Application extends BookHelper {
    private static final String CACHE_TTL = "30mn";

    /**
     * If the user is authenticated, retrieve it's feeds. If the user is not
     * known, display only sample feeds.
     */
    public static void index() {
        // Retrieve the list of feeds for this user
        User user = getUser();
        Set<Feed> sampleFeeds = SampleFeedsHelper.getSampleFeeds();
        if (user != null) {
            List<Feed> feeds = Feed.find("byCreator", user).fetch();
            if (feeds == null) {
                login();
            }
            System.out.println("User " + user.openId + " authenticated successfully.");
            System.out.println("Number of feeds: " + feeds.size());
            render(feeds, sampleFeeds);
        }
        render(sampleFeeds);
    }

    /**
     * Fetch the user from the database.
     * 
     * @return the authenticated User.
     */
    protected static User getUser() {
        User user = User.find("byOpenId", session.get("user")).first();
        return user;
    }

    /**
     * Downloads a feed as an ebook. The feed is identified by its title AND the
     * user.
     * 
     * @param feedTitle
     *            The title of the feed to be rendered as an ebook.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    public static void download(String feedTitle) throws IllegalArgumentException, FeedException, IOException {
        Feed feed = Feed.find("byCreatorAndTitle", getUser(), feedTitle).first();
        downloadBook(feed.title, feed.url);
    }

    /**
     * Downloads a sample feed as an ebook. Sample feeds are not stored like
     * user feeds, they don't need an authentication.
     * 
     * @param feedTitle
     *            The title of the sample feed to be rendered as an ebook.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    public static void downloadSample(String feedTitle) throws IllegalArgumentException, FeedException, IOException {
        URL feedUrl = SampleFeedsHelper.getFeedURL(feedTitle);
        downloadBook(feedTitle, feedUrl);
    }

    /**
     * Render the ebook and send the result as a binary file.
     * 
     * @param feedTitle
     *            The title of the feed which will be used to create the
     *            filename.
     * @param feedUrl
     *            The URL of the feed where we can retrieve RSS data.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    private static void downloadBook(String feedTitle, URL feedUrl) throws IllegalArgumentException, FeedException,
            IOException {
        Document doc = getContent(feedUrl);
        InputStream bookStream = renderBook(feedUrl, doc);
        if (bookStream != null) {
            renderBinary(bookStream, feedTitle + ".epub", "application/epub+zip", false);
        } else {
            flash.error("Could not create book.");
            index();
        }
    }

    /**
     * Authentication is performed before on every action except "public"
     * actions.
     */
    @Before(unless = { "login", "authenticate", "index", "downloadSample" })
    static void checkAuthenticated() {
        if (!session.contains("user")) {
            login();
        }
    }

    /**
     * Display the login form. Nothing interesting here.
     */
    public static void login() {
        render();
    }

    /**
     * Just delete the user data from the session.
     */
    public static void logout() {
        session.remove("user");
        index();
    }

    /**
     * OpenID authentication, implemented for Google openID first, coult ne
     * extended to many other openID providers.
     * 
     * @param opurl
     *            The openID provider URL.
     */
    public static void authenticate(String opurl) {
        if (OpenID.isAuthenticationResponse()) {
            // The openID provider sent us its response.
            UserInfo verifiedUser = OpenID.getVerifiedID();
            if (verifiedUser == null) {
                flash.error("Oops. Authentication has failed");
                login();
            }
            // Retrieve the user and store its data in the session.
            User.connectOrCreate(verifiedUser.id, verifiedUser.extensions.get("email"),
                    verifiedUser.extensions.get("firstname"), verifiedUser.extensions.get("lastname"));
            session.put("user", verifiedUser.id);
            session.put("firstname", verifiedUser.extensions.get("firstname"));
            session.put("lastname", verifiedUser.extensions.get("lastname"));
            session.put("email", verifiedUser.extensions.get("email"));
            index();
        } else {
            // Send the authentication request. For Google, we ask for the user
            // email, firstname and lastname. These 3 fields may vary for other
            // openID providers. The email is the most reliable ID as the unique
            // ID provided in the authentication result varies depending on the
            // referring site.
            OpenID oi = OpenID.id(opurl).required("email", "http://axschema.org/contact/email")
                    .required("firstname", "http://axschema.org/namePerson/first")
                    .required("lastname", "http://axschema.org/namePerson/last");
            if (!oi.verify()) { // will redirect the user
                flash.error("Cannot verify your OpenID");
                login();
            }
        }
    }

    /**
     * An authenticated user asks to add a new feed to its collection.
     * 
     * @param feedtitle
     *            The title of the feed, as requested by the user.
     * @param feedurl
     *            The URL of the RSS feed.
     * @throws MalformedURLException
     */
    public static void addFeedToUser(@Required String feedtitle, @Required String feedurl) throws MalformedURLException {
        validation.url(feedurl);
        validation.maxSize(feedtitle, 20);

        if (validation.hasErrors()) {
            index();
        }

        System.out.println("Find user with openId: " + session.get("user"));
        User user = getUser();
        if (user == null) {
            logout();
        }

        System.out.println("Received new feed - user: " + user.toString() + " - title: " + feedtitle + " - url: "
                + feedurl);
        try {
            System.out.println("Insert new feed - title: " + feedtitle + " - url: " + feedurl);
            new Feed(user, feedtitle, new URL(feedurl)).save();
        } catch (MalformedURLException e) {
            Logger.info("Title: %s, URL: %s", feedtitle, feedurl);
            throw (e);
        }
        index();
    }

    /**
     * The user asks for a feed deletion from its collection.
     * 
     * @param url
     *            The URL of the RSS feed.
     * @throws MalformedURLException
     */
    public static void deleteFeed(String url) throws MalformedURLException {
        Feed.delete(getUser(), new URL(url));
        index();
    }

    /**
     * Retrieve the content of a RSS feed. Content is put in application cache,
     * with a Time To Live defined in {@link #CACHE_TTL}.
     * 
     * @param url
     *            The URL of the RSS feed.
     * @return A DOM representation of the feed.
     */
    private static Document getContent(URL url) {
        String strUrl = url.toString();

        Logger.info("Retrieve content from: %s", strUrl);
        Document contentDoc = Cache.get("doc." + strUrl, Document.class);

        if (contentDoc == null) {
            Logger.info("Content not in cache, fetch it.");
            Promise<WS.HttpResponse> feedResult = WS.url(strUrl).getAsync();
            HttpResponse feedContent = await(feedResult);
            contentDoc = feedContent.getXml();
            Cache.set("doc." + strUrl, contentDoc, CACHE_TTL);
        }
        return contentDoc;
    }

    /**
     * Before every action except downloading files, we detect if a Kobo Touch
     * is used in order to apply some specific CSS styling.
     */
    @Before(unless = { "download", "downloadSample" })
    public static void detectReader() {
        Header UA = request.headers.get("user-agent");

        System.out.println("user-agent: " + UA.value());
        if (UA != null && UA.value().contains("Kobo Touch")) {
            System.out.println("Kobo Touch detected!");
            renderArgs.put("reader", true);
        } else {
            System.out.println("Not a Kobo Touch.");
            renderArgs.put("reader", false);
        }
    }
}