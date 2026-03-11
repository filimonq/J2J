package j2j;

import j2j.model.User;
import j2j.serializer.J2JSerializer;
import j2j.serializer.J2JSerializationException;

public class Main {

    public static void main(String[] args) {
        J2JSerializer serializer = new J2JSerializer();

        User u1 = new User("Anna", 19, true);
        u1.setId(1L);

        User u2 = new User("Bob", 25, false);
        u2.setId(2L);

        System.out.println(serializer.serialize(u1));
        System.out.println(serializer.serialize(u2));


        try {
            serializer.serialize("just a string");
        } catch (J2JSerializationException e) {
            System.out.println("Caught expected: " + e.getMessage());
        }
    }
}
