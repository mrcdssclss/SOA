package org.example.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.example.model.Person;
import org.example.model.EyeColor;
import org.example.repository.PersonRepository;
import jakarta.ws.rs.core.MultivaluedMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequestScoped
public class PersonService {

    @Inject
    private PersonRepository personRepository;

    @Inject
    private Validator validator;

    @Transactional
    public Person create(Person person) {
        if (person == null) {
            throw new BadRequestException("Тело запроса обязательно");
        }
        person.setId(null);
        person.setCreationDate(LocalDate.now());
        validatePerson(person);

        personRepository.save(person);

        return person;
    }

    public Person findById(Integer id) {
        validateId(id);
        return personRepository.findById(id);
    }


    @Transactional
    public Person update(Integer id, Person newPerson) {
        validateId(id);
        if (newPerson == null) {
            throw new BadRequestException("Тело запроса обязательно");
        }

        Person person = personRepository.findById(id);
        if (person == null) {
            return null;
        }

        newPerson.setId(person.getId());
        newPerson.setCreationDate(person.getCreationDate());
        validatePerson(newPerson);

        person.setName(newPerson.getName());
        person.setCoordinates(newPerson.getCoordinates());
        person.setHeight(newPerson.getHeight());
        person.setEyeColor(newPerson.getEyeColor());
        person.setHairColor(newPerson.getHairColor());
        person.setNationality(newPerson.getNationality());
        person.setLocation(newPerson.getLocation());

        return person;
    }

    @Transactional
    public boolean delete(Integer id) {
        validateId(id);
        Person person = personRepository.findById(id);

        if (person == null) {
            return false;
        }

        personRepository.delete(person);
        return true;
    }

    public List<Person> findAll(MultivaluedMap<String, String> parameters) {
        PeopleQuery query;
        try {
            validateSingleParameters(parameters);
            query = PeopleQuery.parse(parameters);
            validatePeopleQuery(query, parameters);
            long offset = (long) query.getPage() * query.getPageSize();
            if (offset > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Смещение page * pageSize превышает " + Integer.MAX_VALUE);
            }
            query.setOffset((int) offset);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
        return personRepository.findAll(query);
    }

    public Person findWithMinEyeColor() {
        return personRepository.findWithMinEyeColor();
    }

    public Person findWithMaxId() {
        return personRepository.findWithMaxId();
    }

    public long countEyeColorsGreater(String color) {
        if (color == null) {
            throw new BadRequestException("Параметр color обязателен");
        }
        EyeColor eyeColor;
        try {
            eyeColor = EyeColor.valueOf(color);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Неизвестный eyeColor: " + color);
        }
        return personRepository.countEyeColorsGreater(eyeColor);
    }

    private void validatePerson(Person person) {
        Set<ConstraintViolation<Person>> violations = validator.validate(person);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("id должен быть больше 0");
        }
    }

    private void validatePeopleQuery(PeopleQuery query, MultivaluedMap<String, String> parameters) {
        if (query.getPage() < 0) {
            throw new IllegalArgumentException("page должен быть >= 0");
        }
        if (query.getPageSize() <= 0) {
            throw new IllegalArgumentException("pageSize должен быть > 0");
        }
        List<String> sortFields = parameters.get("sortBy");
        List<String> orders = parameters.get("order");
        if (sortFields == null && orders != null) {
            throw new IllegalArgumentException("order требует sortBy");
        }
        if (orders != null && orders.size() != sortFields.size()) {
            throw new IllegalArgumentException("Число order должно совпадать с числом sortBy");
        }
        if (sortFields != null) {
            for (String field : sortFields) {
                if (!PeopleQuery.FIELDS.contains(field)) {
                    throw new IllegalArgumentException("Недопустимое поле sortBy: " + field);
                }
            }
        }
        if (orders != null) {
            for (String order : orders) {
                if (!"asc".equals(order) && !"desc".equals(order)) {
                    throw new IllegalArgumentException("order должен быть asc или desc");
                }
            }
        }
    }

    private void validateSingleParameters(MultivaluedMap<String, String> parameters) {
        for (String field : PeopleQuery.FIELDS) {
            if ("name".equals(field)) {
                continue;
            }
            validateSingleParameter(parameters, field);
        }
        validateSingleParameter(parameters, "page");
        validateSingleParameter(parameters, "pageSize");
    }

    private void validateSingleParameter(MultivaluedMap<String, String> parameters, String name) {
        List<String> values = parameters.get(name);
        if (values != null && values.size() != 1) {
            throw new IllegalArgumentException("Параметр " + name + " должен встречаться один раз");
        }
    }
}
