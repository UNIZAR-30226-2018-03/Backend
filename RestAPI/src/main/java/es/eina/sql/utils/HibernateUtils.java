package es.eina.sql.utils;

import es.eina.RestApp;
import es.eina.sql.entities.EntitySong;
import es.eina.sql.entities.EntityToken;
import es.eina.sql.entities.EntityUser;
import es.eina.sql.entities.EntityUserValues;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HibernateUtils {

    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory configureDatabase(String f) {
        if (sessionFactory == null) {
            try {
                ClassLoader loader = RestApp.class.getClassLoader();
                Properties login = new Properties();
                File f2 = new File(loader.getResource(f).toURI());
                if(!f2.exists()){
                    System.out.println("Cannot load " + f2.getAbsolutePath() + " config file...");
                }
                System.out.println("Path: " + f2.getAbsolutePath());
                String line;
                try (FileReader in = new FileReader(f2)) {
                    BufferedReader reader = new BufferedReader(in);
                    while((line = reader.readLine()) != null){
                        String[] lines = line.substring(1).split("=");
                        if(lines.length == 2) {
                            System.out.printf("Loaded key/value: %s, %s.\n", lines[0], lines[1]);
                            login.put(lines[0], lines[1]);
                        }else if(lines.length == 1){
                            System.out.printf("Invalid key/value, only key provided (=): %s.\n", lines[0]);
                        }
                    }
                }catch(Exception e){
                    System.out.println("Cannot load properties file: " + e.getMessage());
                    e.printStackTrace();
                }

                StandardServiceRegistryBuilder registryBuilder =
                        new StandardServiceRegistryBuilder();

                Map<String, Object> settings = new HashMap<>();
                //settings.put(Environment.DRIVER, "org.postgresql.ds.PGSimpleDataSource");
                settings.put(Environment.DRIVER, "org.postgresql.Driver");
                settings.put(Environment.URL, "jdbc:postgresql://" + login.getProperty("host") + "/" + login.getProperty("db"));
                settings.put(Environment.USER, login.getProperty("user"));
                settings.put(Environment.PASS, login.getProperty("pass"));
                settings.put(Environment.HBM2DDL_AUTO, "update");
                settings.put(Environment.SHOW_SQL, false);

                // HikariCP settings

                // Maximum waiting time for a connection from the pool
                settings.put("hibernate.hikari.connectionTimeout", "20000");
                // Minimum number of ideal connections in the pool
                settings.put("hibernate.hikari.minimumIdle", "10");
                // Maximum number of actual connection in the pool
                settings.put("hibernate.hikari.maximumPoolSize", "20");
                // Maximum time that a connection is allowed to sit ideal in the pool
                settings.put("hibernate.hikari.idleTimeout", "300000");

                registryBuilder.applySettings(settings);

                registry = registryBuilder.build();
                MetadataSources sources = new MetadataSources(registry);

                sources.addAnnotatedClass(EntityUser.class);
                sources.addAnnotatedClass(EntityToken.class);
                sources.addAnnotatedClass(EntityUserValues.class);
                sources.addAnnotatedClass(EntitySong.class);

                Metadata metadata = sources.getMetadataBuilder().build();
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception e) {
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new RuntimeException("Cannot access a non-built SessionFactory.");
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}