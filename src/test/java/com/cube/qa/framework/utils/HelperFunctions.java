package com.cube.qa.framework.utils;

import com.github.javafaker.Faker;

public class HelperFunctions {

    private static final Faker faker = new Faker();

    public static String generateRandomEmail() {
        String email = generateRandomUsername().toLowerCase() + "@3sctest.com";
        System.out.println("Generated Email: " + email);
        return email;
    }


    public static String generateRandomFirstName() {
        String name = faker.name().firstName();
        System.out.println("Generated First Name: " + name);
        return name;
    }

    public static String generateRandomLastName() {
        String name = faker.name().lastName().replace("'", "");
        System.out.println("Generated Last Name: " + name);
        return name;
    }

    public static String generateRandomUsername() {
        String base = faker.bothify("????##").toUpperCase();
        String username = base + faker.number().randomNumber(2, true);
        System.out.println("Generated Username: " + username);
        return username;
    }

    public static String generatePassword() {
        String password = faker.internet().password(8, 12, true, true, false);
        System.out.println("Generated Password: " + password);
        return password;
    }
}
