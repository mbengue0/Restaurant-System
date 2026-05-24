package com.daust.restaurant.application;

/**
 * Thrown when a Category cannot be deactivated because it still has active MenuItems (FR13). This
 * is the Application-level cross-aggregate guard called out in CLAUDE.md ("Deferred cross-aggregate
 * checks") — Category.deactivate() in Domain is unconditional.
 */
public class CategoryHasActiveItemsException extends RuntimeException {

    public CategoryHasActiveItemsException(String message) {
        super(message);
    }
}
