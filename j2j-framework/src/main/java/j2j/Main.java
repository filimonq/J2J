package j2j;

import j2j.filter.*;
import j2j.id.CounterIdStrategy;
import j2j.model.Fullname;
import j2j.model.User;
import j2j.model.UserTest;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        java.nio.file.Files.write(
                java.nio.file.Path.of("storage.json"),
                new byte[0],
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );

        PersistenceManager manager = new PersistenceManager("storage.json", new CounterIdStrategy());

        System.out.println("=== SHIFT 1: SAVING DATA ===");
        Fullname fn1 = new Fullname("Ivan", "Ivanov");
        User u1 = new User(fn1, 20, true);

        Fullname fn2 = new Fullname("Petr", "Petrov");
        User u2 = new User(fn2, 30, false);

        Fullname fn3 = new Fullname("Ivan", "Ivanov");
        UserTest u3 = new UserTest(fn3, 222, true);

        manager.save(fn1);
        manager.save(u1);
        manager.save(fn2);
        manager.save(u2);
        manager.save(fn3);
        manager.save(u3);

        u1.setAge(10);
        manager.save(u1);

        Thread.sleep(3000);
        System.out.println("Flushing to disk...");
        manager.flush();
        Thread.sleep(5000);

        System.out.println("\n=== SHIFT 2: IDENTITY MAP CHECK ===");
        User cachedUser = (User) manager.getById(2L);
        System.out.println("Same object in memory: " + (u1 == cachedUser));

        System.out.println("\n=== SHIFT 3: UPDATE & COMPACT ===");
        System.out.println("Updating Ivan's age to 21...");
        u1.setAge(21);
        manager.save(u1);
        manager.flush();
        Thread.sleep(5000);

        System.out.println("Running compaction (cleaning old versions)...");
        manager.compact();
        Thread.sleep(5000);

        System.out.println("\n=== SHIFT 4: OPTIMIZED FILTER TEST ===");
        PersistenceManager filterManager = new PersistenceManager("storage.json", new CounterIdStrategy());

        JsonFilter myFilter = new AndFilter(
                new FieldEqualsFilter("type", "User"),
                new FieldEqualsFilter("active", "true")
        );

        System.out.println("Loading active users with optimized filter...");
        List<Object> activeUsers = filterManager.loadWithFilter(myFilter);

        for (Object obj : activeUsers) {
            User u = (User) obj;
            System.out.println("Found Active User: " + u.getName().name + " " + u.getName().surname + ", age: " + u.getAge());
        }

        JsonFilter orFilter = new OrFilter(
                new FieldEqualsFilter("type", "User"),
                new FieldEqualsFilter("active", "true")
        );

        System.out.println("\n--- Testing OR Filter (Type=User OR active=true) ---");
        List<Object> allUsers = filterManager.loadWithFilter(orFilter);
        for (Object obj : allUsers) {
            if (obj instanceof User u) {
                System.out.println("Found User: " + u.getName().name + ", Age: " + u.getAge());
            } else if (obj instanceof UserTest ut) {
                System.out.println("Found UserTest: " + ut.getName().name + ", Value: " + ut.getAge());
            }
        }

        System.out.println("\n=== SUCCESSFULLY ===");
    }
}