package org.example.service;

import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;
import org.example.model.Country;
import org.example.model.EyeColor;
import org.example.model.HairColor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class PeopleQuery {
    public static final Set<String> FIELDS = Set.of(
            "id", "name", "coordinates.x", "coordinates.y", "creationDate",
            "height", "eyeColor", "hairColor", "nationality",
            "location.x", "location.y", "location.z");

    private int offset;
    private int page;
    private int pageSize;
    private final Map<String, Object> filters = new LinkedHashMap<>();
    private final List<String> nameFilters = new ArrayList<>();
    private final List<String> sortFields = new ArrayList<>();
    private final List<Boolean> descending = new ArrayList<>();

    public static PeopleQuery parse(MultivaluedMap<String, String> parameters) {
        PeopleQuery query = new PeopleQuery();
        query.page = integer(single(parameters, "page", "0"), "page");
        query.pageSize = integer(single(parameters, "pageSize", "20"), "pageSize");
        for (String field : FIELDS) {
            if (!"name".equals(field) && parameters.containsKey(field)) {
                query.filters.put(field, filter(field, single(parameters, field, null)));
            }
        }
        List<String> names = parameters.get("name");
        if (names != null) {
            for (String name : names) {
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("name не должен быть пустым");
                }
                query.nameFilters.add(name);
            }
        }
        List<String> fields = parameters.get("sortBy");
        List<String> orders = parameters.get("order");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                query.sortFields.add(fields.get(i));
                String order = orders == null || i >= orders.size() ? "asc" : orders.get(i);
                query.descending.add("desc".equals(order));
            }
        }
        if (!query.sortFields.contains("id")) {
            query.sortFields.add("id");
            query.descending.add(false);
        }
        return query;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    private static String single(
            MultivaluedMap<String, String> parameters,
            String name,
            String fallback) {

        List<String> values = parameters.get(name);

        if (values == null || values.isEmpty()) {
            return fallback;
        }

        return values.get(0);
    }

    private static int integer(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " должен быть целым числом в диапазоне Integer");
        }
    }

    private static Object filter(String field, String value) {
        try {
            return switch (field) {
                case "id", "height" -> {
                    int number = integer(value, field);
                    if (number < 0) {
                        throw new IllegalArgumentException(field + " должен быть >= 0");
                    }
                    yield number;
                }
                case "coordinates.x" -> {
                    long number = Long.parseLong(value);
                    if (number > 557) {
                        throw new IllegalArgumentException("coordinates.x должен быть <= 557");
                    }
                    yield number;
                }
                case "coordinates.y" -> {
                    double number = Double.parseDouble(value);
                    if (!Double.isFinite(number)) {
                        throw new IllegalArgumentException("coordinates.y должен быть конечным числом");
                    }
                    yield number;
                }
                case "location.x" -> Long.parseLong(value);
                case "location.y" -> {
                    float number = Float.parseFloat(value);
                    if (!Float.isFinite(number)) {
                        throw new IllegalArgumentException("location.y должен быть конечным числом Float");
                    }
                    yield number;
                }
                case "location.z" -> integer(value, field);
                case "creationDate" -> {
                    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                        throw new IllegalArgumentException("creationDate должен иметь формат YYYY-MM-DD");
                    }
                    yield LocalDate.parse(value);
                }
                case "eyeColor" -> enumValue(EyeColor.class, value, field);
                case "hairColor" -> enumValue(HairColor.class, value, field);
                case "nationality" -> "null".equals(value) ? null : enumValue(Country.class, value, field);
                default -> throw new IllegalArgumentException("Недопустимое поле фильтра: " + field);
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное число для " + field + ": " + value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректная creationDate: ожидается существующая дата YYYY-MM-DD");
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " должен быть одним из " + List.of(type.getEnumConstants()));
        }
    }
}
