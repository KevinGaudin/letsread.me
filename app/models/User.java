package models;

import java.util.*;

import javax.mail.Session;
import javax.persistence.*;

import play.db.jpa.*;

@Entity
public class User extends Model {

    @Column(unique=true)
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
	if(result == null) {
	    // User not found, create one
	    result = new User(openId, email, firstname, lastname);
	    result.save();
	} else if(!result.email.equals(email)
		|| !result.firstname.equals(firstname)
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