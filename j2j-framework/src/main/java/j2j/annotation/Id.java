package j2j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the unique identifier of a persistent object.
 * The field must be of type Long.
 * J2J will auto-assign an ID when the object is saved for the first time.
 *
 * Rules:
 * - Exactly one field per @Persistent class must be annotated with @Id
 * - The field type must be Long (not primitive long)
 * - If the field value is null at save time, J2J assigns the next available ID
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
}
