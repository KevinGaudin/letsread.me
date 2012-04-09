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
package models;

import java.net.URL;
import java.util.*;
import javax.persistence.*;

import play.db.jpa.*;

/**
 * All the data we need to store about feeds.
 */
@Entity
public class Feed extends Model implements Comparable<Feed> {

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

    /**
     * Alphabetical order.
     */
    @Override
    public int compareTo(Feed o) {
        return title.compareTo(o.title);
    }
}