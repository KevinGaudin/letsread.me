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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import models.Feed;

/**
 * Sample feeds are managed in a specific helper class to allow moving to a
 * different storage/configuration later... For the moment, they are
 * "hardcoded" here.
 */
public class SampleFeedsHelper {
    private static Map<String, Feed> sampleFeeds = new HashMap<String, Feed>();
    static {
        try {
            sampleFeeds.put("FMyLife", new Feed(null, "FMyLife", new URL("http://feeds.feedburner.com/fmylife")));
            sampleFeeds.put("Gizmodo-France", new Feed(null, "Gizmodo-France", new URL("http://www.gizmodo.fr/feed")));
            sampleFeeds.put("TheVerge",
                    new Feed(null, "TechCrunch", new URL("http://feeds.feedburner.com/TechCrunch/")));
            sampleFeeds.put("Mashable", new Feed(null, "Mashable", new URL("http://feeds.feedburner.com/Mashable")));
            sampleFeeds.put("WiredTopStories", new Feed(null, "WiredTopStories", new URL(
                    "http://feeds.wired.com/wired/index")));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static Set<Feed> getSampleFeeds() {
        return new TreeSet<Feed>(sampleFeeds.values());
    }

    public static URL getFeedURL(String feedTitle) {
        return sampleFeeds.get(feedTitle).url;
    }

}
