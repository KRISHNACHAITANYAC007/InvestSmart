package com.example.investsmart.chat.agent;

import java.util.*;

/** Schema/permission boundary only. AI-1 registers no application tools and executes none. */
public final class ToolRegistry {
    public record Definition(String name, Set<String> requiredArguments) {
        public Definition {
            if (name == null || !name.matches("[a-zA-Z][a-zA-Z0-9]{0,63}")) throw new IllegalArgumentException("Invalid tool name");
            requiredArguments = Set.copyOf(requiredArguments);
            if (requiredArguments.stream().anyMatch(k -> !k.matches("[a-zA-Z][a-zA-Z0-9]{0,63}")
                    || Set.of("userid", "email", "principal", "accountid").contains(k.toLowerCase(Locale.ROOT)))) {
                throw new IllegalArgumentException("Identity arguments are forbidden");
            }
        }
    }
    private final Map<String, Definition> definitions;
    public ToolRegistry(List<Definition> tools) {
        var entries = new HashMap<String, Definition>();
        for (var tool : tools) if (entries.putIfAbsent(tool.name(), tool) != null) throw new IllegalArgumentException("Duplicate tool");
        definitions = Map.copyOf(entries);
    }
    public Definition validate(String name, Map<String, String> arguments) {
        var tool = name == null ? null : definitions.get(name);
        if (tool == null || arguments == null || !arguments.keySet().equals(tool.requiredArguments())
                || arguments.values().stream().anyMatch(v -> v == null || v.isBlank() || v.length() > 1000)) {
            throw new IllegalArgumentException("Tool request not permitted");
        }
        return tool;
    }
    public int size() { return definitions.size(); }
}
