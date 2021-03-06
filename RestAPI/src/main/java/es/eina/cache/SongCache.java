package es.eina.cache;

import es.eina.sql.entities.EntitySong;
import es.eina.sql.utils.HibernateUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SongCache {

    private static Map<Long, EntitySong> songs = new ConcurrentHashMap<>();

    /**
     * Removes from cache all entities that has been stored first and have expired.
     * A value is considered expired if isEntityValid() returns false (it is the entity has benn stored "x" ms before).
     *
     * @param time : Current epoch time in ms.
     */
    public static void cleanUp(long time) {
        List<EntitySong> remove = new ArrayList<>();

        for (EntitySong data : songs.values()) {
            if (!data.isEntityValid(time)) {
                remove.add(data);
            }
        }

        saveEntities(remove);
    }

    public static void forceSave() {
        saveEntities(songs.values());
    }

    private static void saveEntities(Collection<EntitySong> remove) {
        Transaction tr = null;
        try (Session session = HibernateUtils.getSessionFactory().openSession()) {
            for (EntitySong data : remove) {
                if (data.isDirty()) {
                    try {
                        tr = session.beginTransaction();
                        session.saveOrUpdate(data);
                        tr.commit();
                        songs.remove(data.getId());
                    } catch (Exception e) {
                        if (tr != null) {
                            tr.rollback();
                        }
                    }
                }
            }
        }
    }


    public static void addSong(EntitySong song) {
        long id = song.getId();
        if (!songs.containsKey(id)) {
            songs.put(id, song);
        }
    }

    private static EntitySong loadSong(long songId) {
        EntitySong song = null;
        Transaction tr = null;
        try (Session session = HibernateUtils.getSessionFactory().openSession()) {
            tr = session.beginTransaction();
            song = session.load(EntitySong.class, songId);
            tr.commit();
            addSong(song);
        } catch (Exception e) {
            if (tr != null) {
                tr.rollback();
            }
        }
        return song;
    }

    public static EntitySong getSong(long songId) {
        EntitySong song = songs.get(songId);
        if (song == null) {
            song = loadSong(songId);
        }

        return song;
    }

    public static boolean deleteSong(EntitySong song) {
        Transaction tr = null;
        try (Session session = HibernateUtils.getSessionFactory().openSession()) {
            long id = song.getId();
            tr = session.beginTransaction();
            session.delete(song);
            tr.commit();
            songs.remove(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (tr != null && tr.isActive()) {
                tr.rollback();
            }
        }

        return false;
    }

}
