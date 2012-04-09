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

import java.util.*;

import javax.mail.Session;
import javax.persistence.*;

import play.db.jpa.*;

/**
 * Our users.
 */
@Entity
public class User extends Model {

    @Column(unique = true)
    public String openId;
    public String email;
    public String firstname;
    public String lastname;
    public boolean isAdmin;

    public User(String openId, String email, String firstname, String lastname) {
        this.openId = openId;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public static User connectOrCreate(String openId, String email, String firstname, String lastname) {
        User result = find("byOpenId", openId).first();
        if (result == null) {
            // User not found, create one
            result = new User(openId, email, firstname, lastname);
            result.save();
        } else if (!result.email.equals(email) || !result.firstname.equals(firstname)
                || !result.lastname.equals(lastname)) {
            // User found but its data have changed. Update them.
            result.email = email;
            result.firstname = firstname;
            result.lastname = lastname;
            result.save();
        }
        return result;
    }
}