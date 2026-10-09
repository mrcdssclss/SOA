package org.example.repository;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Person;
import org.example.model.EyeColor;
import org.example.model.HairColor;
import org.example.service.PeopleQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Locale;

import java.util.List;

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
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
        Root<Person> person = criteria.from(Person.class);
        criteria.select(person)
                .orderBy(builder.asc(enumPosition(builder, person.get("eyeColor"), EyeColor.values())),
                        builder.asc(person.get("id")));
        List<Person> result = entityManager.createQuery(criteria).setMaxResults(1).getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public Person findWithMaxId() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
        Root<Person> person = criteria.from(Person.class);
        criteria.select(person).orderBy(builder.desc(person.get("id")));
        List<Person> result = entityManager.createQuery(criteria).setMaxResults(1).getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public long countEyeColorsGreater(EyeColor color) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Person> person = criteria.from(Person.class);
        Path<EyeColor> eyeColor = person.get("eyeColor");
        criteria.select(builder.count(person))
                .where(builder.isNotNull(eyeColor),
                        builder.greaterThan(enumPosition(builder, eyeColor, EyeColor.values()), color.ordinal()));
        return entityManager.createQuery(criteria).getSingleResult();
    }


    public List<Person> findAll(PeopleQuery options) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
        Root<Person> person = criteria.from(Person.class);
        List<Predicate> predicates = new ArrayList<>();
        List<FilterParameter<?>> parameters = new ArrayList<>();
        for (String name : options.getNameFilters()) {
            FilterParameter<String> parameter = filterParameter(builder, String.class,
                    "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%");
            Path<String> namePath = person.get("name");
            predicates.add(builder.like(builder.lower(namePath), parameter.expression(), '\\'));
            parameters.add(parameter);
        }
        options.getFilters().forEach((field, value) -> {
            Path<?> path = fieldPath(person, field);
            if (value == null) {
                predicates.add(builder.isNull(path));
            } else {
                Class<?> type = path.getJavaType();
                if (type == double.class) {
                    type = Double.class;
                }
                FilterParameter<?> parameter = filterParameter(builder, type, value);
                predicates.add(builder.equal(path, parameter.expression()));
                parameters.add(parameter);
            }
        });
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < options.getSortFields().size(); i++) {
            String field = options.getSortFields().get(i);
            Path<?> path = fieldPath(person, field);
            Expression<?> expression = path;
            if ("eyeColor".equals(field)) {
                expression = enumPosition(builder, path, EyeColor.values());
            } else if ("hairColor".equals(field)) {
                expression = enumPosition(builder, path, HairColor.values());
            } else if ("nationality".equals(field)) {
                orders.add(builder.asc(builder.<Integer>selectCase()
                        .when(builder.isNull(path), 1).otherwise(0)));
            }
            orders.add(options.getDescending().get(i)
                    ? builder.desc(expression) : builder.asc(expression));
        }
        criteria.select(person)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(orders);
        TypedQuery<Person> query = entityManager.createQuery(criteria);
        parameters.forEach(parameter -> parameter.bind(query));
        return query.setFirstResult(options.getOffset())
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
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Expression<Integer> enumPosition(CriteriaBuilder builder, Path<?> path, Enum<?>[] values) {
        CriteriaBuilder.Case<Integer> position = builder.selectCase();
        for (int i = 0; i < values.length; i++) {
            position.when(builder.equal(path, values[i]), i);
        }
        return position.otherwise(values.length);
    }

    private <T> FilterParameter<T> filterParameter(CriteriaBuilder builder, Class<T> type, Object value) {
        return new FilterParameter<>(builder.parameter(type), type.cast(value));
    }

    private record FilterParameter<T>(ParameterExpression<T> expression, T value) {
        void bind(TypedQuery<Person> query) {
            query.setParameter(expression, value);
        }
    }
}
