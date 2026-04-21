package com.cube.qa.framework.testdata.loader;

import com.cube.qa.framework.testdata.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

public class UserDataLoader {

    private static String environment;

    // Called by BaseTest to set the environment
    public static void setEnvironment(String env) {
        environment = env;
    }

    public static List<User> loadUsers() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String fileName = String.format("testdata/%s/users.json", environment.toLowerCase());
            InputStream is = UserDataLoader.class.getClassLoader().getResourceAsStream(fileName);
            return mapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load user data for environment: " + environment, e);
        }
    }

    public static User findUser(Predicate<User> condition) {
        return loadUsers().stream()
                .filter(condition)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No user matched the given condition."));
    }
}
