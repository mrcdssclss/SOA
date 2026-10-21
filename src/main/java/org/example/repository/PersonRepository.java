package org.example.repository;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.model.EyeColor;
import org.example.model.Person;
import org.example.service.PeopleQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequestScoped
public class PersonRepository {

    @PersistenceContext(unitName = "PeoplePU")
    private EntityManager entityManager;

    public void save(Person person) {
        entityManager.persist(person);
    }

    public Person findById(Integer id) {
        return entityManager.find(Person.class, id);
    }

    public void delete(Person person) {
        entityManager.remove(person);
    }

    public Person findWithMinEyeColor() {
        return entityManager.createQuery(
                        "SELECT p FROM Person p ORDER BY p.eyeColor ASC, p.id ASC",
                        Person.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public Person findWithMaxId() {
        return entityManager.createQuery(
                        "SELECT p FROM Person p ORDER BY p.id DESC",
                        Person.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    public long countEyeColorsGreater(EyeColor color) {
        List<EyeColor> colors = new ArrayList<>();
        for (EyeColor value : EyeColor.values()) {
            if (value.ordinal() > color.ordinal()) {
                colors.add(value);
            }
        }
        if (colors.isEmpty()) {
            return 0;
        }

        return entityManager.createQuery(
                        "SELECT COUNT(p) FROM Person p WHERE p.eyeColor IN :colors",
                        Long.class)
                .setParameter("colors", colors)
                .getSingleResult();
    }

    public List<Person> findAll(PeopleQuery options) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
        Root<Person> person = criteria.from(Person.class);

        List<Predicate> predicates = new ArrayList<>();

        for (String name : options.getNameFilters()) {
            String value = "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%";
            predicates.add(builder.like(
                    builder.lower(person.get("name")), value, '\\'));
        }

        options.getFilters().forEach((field, value) -> {
            Path<?> path = fieldPath(person, field);
            if (value == null) {
                predicates.add(builder.isNull(path));
            } else {
                predicates.add(builder.equal(path, value));
            }
        });

        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < options.getSortFields().size(); i++) {
            String field = options.getSortFields().get(i);
            Path<?> path = fieldPath(person, field);

            if (options.getDescending().get(i)) {
                orders.add(builder.desc(path));
            } else {
                orders.add(builder.asc(path));
            }
        }

        criteria.select(person)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(orders);

        return entityManager.createQuery(criteria)
                .setFirstResult(options.getOffset())
                .setMaxResults(options.getPageSize())
                .getResultList();
    }

    private Path<?> fieldPath(Root<Person> person, String field) {
        Path<?> path = person;

        for (String part : field.split("\\.")) {
            path = path.get(part);
        }

        return path;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
