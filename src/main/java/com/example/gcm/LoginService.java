package com.example.gcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapts Lab 1's validator and Lab 2's password matching, without duplicating rules. */
public final class LoginService {
    private final Map<String, List<User>> users;

    public LoginService(List<User> input) {
        Map<String, List<User>> grouped = new HashMap<>();
        for (User user : input) {
            grouped.computeIfAbsent(user.getUsername(), key -> new ArrayList<>()).add(user);
        }
        grouped.replaceAll((email, list) -> List.copyOf(list));
        users = Map.copyOf(grouped);
    }

    public Set<String> usernames() {
        return users.keySet();
    }

    public boolean authenticate(String email, String password) throws InvalidUserException {
        new User(email, password); // Lab 1 validates; LoginTask catches the checked exception.
        return users.getOrDefault(email, List.of()).stream()
                .anyMatch(user -> user.matches(email, password));
    }
}
