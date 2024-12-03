package org.example.OOD.Models;

import java.util.List;
import java.util.stream.Collectors;

public class Category {

    private String name;

    // Constructor
    public Category(String name) {
        this.name = name;
    }

    // Getter for the category name
    public String getName() {
        return name;
    }

    // Method to get predefined categories as a list of Category objects
    public static List<Category> getCategories() {
        return List.of(
                        "General",
                        "Tech",
                        "Entertainment",
                        "Business",
                        "Sports",
                        "Politics",
                        "Travel",
                        "Food",
                        "Health"
                ).stream()
                .map(Category::new) // Convert each String to a Category object
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name; // Useful for debugging and printing
    }
}
