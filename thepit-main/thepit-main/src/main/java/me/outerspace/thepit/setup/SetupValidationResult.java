package me.outerspace.thepit.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SetupValidationResult {
    private final List<String> errors = new ArrayList<>();

    public void add(String error) { errors.add(error); }
    public boolean valid() { return errors.isEmpty(); }
    public List<String> errors() { return Collections.unmodifiableList(errors); }
}
