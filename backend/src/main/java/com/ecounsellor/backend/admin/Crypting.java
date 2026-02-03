package com.ecounsellor.backend.admin;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Crypting {
	public static void main(String[] args) {
	    BCryptPasswordEncoder encoder =
	        new BCryptPasswordEncoder();

	    System.out.println(
	        encoder.encode("admin123")
	    );
	}

}
