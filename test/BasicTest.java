import org.junit.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {

    @Before
    public void setup() {
	Fixtures.deleteDatabase();
    }

    @Test
    public void createAndRetrieveUser() {
	// Create a new user and save it
	new User("http://test.openid/bobuser", "bob@gmail.com", "Bob", "Jones").save();

	// Retrieve the user by openId
	User bob = User.find("byOpenId", "http://test.openid/bobuser").first();

	// Test
	assertNotNull(bob);
	assertEquals("Bob", bob.firstname);
    }

    @Test
    public void tryConnectAsUser() {
	// Test
	assertNotNull(User.connectOrCreate("http://test.openid/bobuser", "bob@gmail.com", "Bob", "Jones"));
    }

    @Test
    public void createFeed() {
	// Create a new user and save it
	User bob = User.connectOrCreate("http://test.openid/bobuser", "bob@gmail.com", "Bob", "Jones");
	URL url = null;
	try {
	    url = new URL("http://www.google.fr");
	} catch (MalformedURLException e) {
	    assertTrue(false);
	}

	// Create a new feed
	new Feed(bob, "The Google Feed", url).save();

	// Test that the post has been created
	assertEquals(1, Feed.count());

	// Retrieve all posts created by Bob
	List<Feed> bobFeeds = Feed.find("byUrl", url).fetch();

	// Tests
	assertEquals(1, bobFeeds.size());
	Feed firstPost = bobFeeds.get(0);
	assertNotNull(firstPost);
	assertEquals(bob, firstPost.creator);
	assertEquals("The Google Feed", firstPost.title);
	assertNotNull(firstPost.createdAt);
    }

}
