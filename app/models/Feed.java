package models;
 
import java.net.URL;
import java.util.*;
import javax.persistence.*;
 
import play.db.jpa.*;
 
@Entity
public class Feed extends Model implements Comparable<Feed>{
 
    public String title;
    public Date createdAt;
    public URL url;
        
    @ManyToOne
    public User creator;
    
    public Feed(User creator, String title, URL url) {
        this.creator = creator;
        this.title = title;
        this.url = url;
        this.createdAt = new Date();
    }
 
    public static void delete(User creator, URL url) {
	Feed feed = Feed.find("byCreatorAndUrl", creator, url).first();
	feed.delete();
    }

    @Override
    public int compareTo(Feed o) {
	return title.compareTo(o.title);
    }
}