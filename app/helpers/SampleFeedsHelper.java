package helpers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import models.Feed;

public class SampleFeedsHelper {
    private static Map<String, Feed> sampleFeeds = new HashMap<String, Feed>();
    static {
	try {
	    sampleFeeds.put("FMyLife", new Feed(null, "FMyLife", new URL("http://feeds.feedburner.com/fmylife")));
        sampleFeeds.put("Gizmodo-France", new Feed(null, "Gizmodo-France", new URL("http://www.gizmodo.fr/feed")));
	    sampleFeeds.put("TheVerge", new Feed(null, "TechCrunch", new URL("http://feeds.feedburner.com/TechCrunch/")));
	    sampleFeeds.put("Mashable", new Feed(null, "Mashable", new URL("http://feeds.feedburner.com/Mashable")));
	    sampleFeeds.put("WiredTopStories", new Feed(null, "WiredTopStories", new URL("http://feeds.wired.com/wired/index")));
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
    }
    
    public static Set<Feed> getSampleFeeds(){
	return new TreeSet<Feed>(sampleFeeds.values());
    }
    
    public static URL getFeedURL(String feedTitle) {
	return sampleFeeds.get(feedTitle).url;
    }

}
