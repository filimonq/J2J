package j2j;

import j2j.deserializer.J2JDeserializer;
import j2j.id.CounterIdStrategy;
import j2j.model.Fullname;
import j2j.model.User;

public class Main {

    public static void main(String[] args) {
        PersistenceManager manager =
                new PersistenceManager("storage.json", new CounterIdStrategy());

        Fullname f1 = new Fullname("Anna", "Ivanova");
        Fullname f2 = new Fullname("Ben", "Johnson");

        manager.save(f1);
        manager.save(f2);

        User u1 = new User(f1, 19, true);
        User u2 = new User(f2, 25, false);

        manager.save(u1);
        manager.save(u2);
        manager.flush();


        manager.loadAll();
        User newU1 = (User) manager.getById(3L);
        User newU2 = (User) manager.getById(4L);
        System.out.println(newU1.getName().name + newU1.getName().surname);
        System.out.println(newU2.getName().name + newU2.getName().surname);

        // Identity Map и Update
        u1.setAge(21);
        manager.save(u1);
        manager.flush();

        System.out.println("Identity Check: " + (u1 == manager.getById(3L)));

        PersistenceManager manager2 =
                new PersistenceManager("storage.json", new CounterIdStrategy());
        manager2.loadAll();

        User loadedUser = (User) manager2.getById(3L);
        System.out.println("Loaded Age: " + loadedUser.getAge());
        System.out.println("Update Logic Success: " + (loadedUser.getAge() == 21));
    }
}
