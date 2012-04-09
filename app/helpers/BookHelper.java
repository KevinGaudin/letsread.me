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
package helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;

import org.w3c.dom.Document;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import play.vfs.VirtualFile;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * Contains all the logic about creating an ebook from an RSS DOM Document.
 * Extends Controller for an easier integration in the Application controller.
 */
public class BookHelper extends Controller {

    /**
     * Renders the feed as an ebook in a ByteArray and returns this data as a
     * stream.
     * 
     * @param feedUrl
     *            The URL of the book.
     * @param doc
     *            The DOM Document retrieved from the feed URL
     * @return An InputStream providing the binary data of the rendered ebook.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    protected static InputStream renderBook(URL feedUrl, Document doc) throws IllegalArgumentException, FeedException,
            IOException {

        Book book = null;
        book = createBookFromFeed(feedUrl, doc);

        if (book != null) {
            EpubWriter epubWriter = new EpubWriter();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                epubWriter.write(book, bos);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return new ByteArrayInputStream(bos.toByteArray());

        }
        return null;
    }

    /**
     * Generate an ebook from an RSS DOM Document.
     * 
     * @param url
     *            The URL from where the Document was fetched (used only to set
     *            the author metadata)
     * @param doc
     *            The DOM Document of the feed.
     * @return An ebook.
     * @throws IllegalArgumentException
     * @throws FeedException
     * @throws IOException
     */
    private static Book createBookFromFeed(URL url, Document doc) throws IllegalArgumentException, FeedException,
            IOException {
        Book book = new Book();
        // start parsing our feed and have the above onItem methods called
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(doc);

        System.out.println(feed);

        // Set the title
        book.getMetadata().addTitle(feed.getTitle());

        // Add an Author
        String author = feed.getAuthor();
        if (author == null || "".equals(author.trim())) {
            author = url.getHost();
        }
        book.getMetadata().addAuthor(new Author(author));

        if (feed.getPublishedDate() != null) {
            book.getMetadata().addDate(new nl.siegmann.epublib.domain.Date(feed.getPublishedDate()));
        }

        if (feed.getDescription() != null) {
            book.getMetadata().addDescription(feed.getDescription());
        }

        if (feed.getCopyright() != null) {
            book.getMetadata().getRights().add(feed.getCopyright());
        }

        // Set cover image - This has never worked.
        // if (feed.getImage() != null) {
        // System.out.println("There is an image for the feed");

        // Promise<HttpResponse> futureImgResponse =
        // WS.url(feed.getImage().getUrl()).getAsync();
        // HttpResponse imgResponse = await(futureImgResponse);
        // System.out.println("Content-type: " + imgResponse.getContentType());
        // if (imgResponse.getContentType().startsWith("image/")) {
        // String extension =
        // imgResponse.getContentType().substring("image/".length());
        // InputStream imageStream = imgResponse.getStream();
        // book.getMetadata().setCoverImage(new Resource(imageStream, "cover." +
        // extension));

        // System.out.println("Using default cover");
        // imageStream =
        // VirtualFile.fromRelativePath("assets/cover.png").inputstream();
        // if (imageStream != null) {
        // System.out.println("Using default cover");
        // book.getMetadata().setCoverImage(new Resource(imageStream,
        // "cover.png"));
        // } else {
        // System.out.println("Could not load default cover");
        // }

        // }
        // }

        int entryNumber = 0;
        List<SyndEntry> entries = feed.getEntries();

        for (SyndEntry entry : entries) {

            StringBuilder title = new StringBuilder(100);
            if (entry.getTitle() != null) {
                title.append(entry.getTitle());
            }
            if (entry.getAuthor() != null) {
                title.append(" - ").append(entry.getAuthor());
            }
            StringBuilder content = new StringBuilder();

            // Add title inside text
            content.append("<h2>").append(title).append("</h2>");

            if (entry.getDescription() != null) {
                SyndContent syndContent = (SyndContent) entry.getDescription();
                if (!syndContent.getType().contains("html")) {
                    content.append("<pre>\n");
                }
                content.append(syndContent.getValue());
                if (!syndContent.getType().contains("html")) {
                    content.append("\n</pre>");
                }
                content.append("<hr/>");
            }

            if (entry.getContents().size() > 0) {
                SyndContent syndContent = (SyndContent) entry.getContents().get(0);
                if (!syndContent.getType().contains("html")) {
                    content.append("<pre>\n");
                }
                content.append(syndContent.getValue());
                if (!syndContent.getType().contains("html")) {
                    content.append("\n</pre>");
                }
            }
            String strContent = clean(content.toString());
            // Add Chapter
            try {
                entryNumber++;
                book.addSection(title.toString(), new Resource(new StringReader(strContent), "entry" + entryNumber
                        + ".xhtml"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return book;
    }

    /**
     * Get rid of some data which have caused parsing/rendering issues.
     * 
     * @param content
     * @return
     */
    private static String clean(String content) {
        // XHTML tags we don't want to keep
        String stripTags = "(" + "<(img|IMG).*?/>" // images (self closed)
                + "|" + "<(img|IMG).*?>.*?</(img|IMG)>" + "|" + "<(script|SCRIPT).*?>.*?</(script|SCRIPT)>" // scripts
                + "|" + "<(object|OBJECT).*?>.*?</(object|OBJECT)>" // objects
                + ")";

        // Wrap content in an XHTML header
        content = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
                + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\"> "
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">" + "<head></head><body>"
                + content.replaceAll("<[aA].*?>(.*?)</[aA]>", "$1") // Strip
                                                                    // links but
                                                                    // keep
                                                                    // their
                                                                    // content
                        .replaceAll(stripTags, " ") // remove unwanted elements
                        .replaceAll("</[aA]>", "") // Some unclosed link tags
                                                   // have been found in a
                                                   // Mashable RSS feed...
                + "</body></html>";
        return content;
    }
}
