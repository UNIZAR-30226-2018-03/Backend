package es.eina.sql.entities;

import es.eina.sql.utils.HibernateUtils;
import org.hibernate.Session;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.*;

@Entity(name="song")
@Table(name="songs")
public class EntitySong extends EntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id",nullable = false)
    private Long id;

    @Column(name = "user_id",nullable = false)
    private long userId;

    @Column(name = "title",nullable = false)
    private String title;

    @Column(name = "country", length = 3,nullable = false)
    private String country;

    @Column(name = "upload_time",nullable = false)
    private long uploadTime;

    @Column(name = "times_listened", nullable = false)
    private long listened;

    @Column(name = "lyrics")
    private String lyrics;  //ruta a la letra de cancion

    @ManyToOne(cascade=CascadeType.ALL)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private EntityUser user;

    @ManyToOne(cascade=CascadeType.ALL)
    @JoinColumn(name = "album")
    private EntityAlbum album;

    @ManyToMany(mappedBy = "songsLiked")
    private Set<EntityUser> usersLikers = new HashSet<>();

    @Column(name = "likes", insertable=false, updatable=false)
    private long likes;

    @ManyToMany(mappedBy = "songsFaved")
    private Set<EntityUser> usersFavers = new HashSet<>();

    @ManyToMany(mappedBy = "songsListened")
    private Set<EntityUser> usersListeners = new HashSet<>();


    /**
     * DO NOT use this method as it can only be used by Hibernate
     */
    public EntitySong(){update();}

    public EntitySong(EntityUser author, String title, String country) {
        this.userId = author.getId();
        this.title = title;
        this.country = country;
        this.uploadTime = System.currentTimeMillis();
        update();
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getCountry() {
        return country;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public String getLyrics() {
        if (this.lyrics != null) return lyrics;
        else return "No Lyrics";
    }

    public boolean isSongLiked(EntityUser user){ return this.usersLikers.contains(user); }

    public boolean likeSong(EntityUser user){ if (this.usersLikers.add(user)){ this.likes++; return true;}else{ return false; }}

    public boolean unlikeSong(EntityUser user){ if (this.usersLikers.remove(user)){ this.likes--; return true;}else{ return false; }}

    public long getLikes(){ return this.likes;}

    public boolean isSongFaved(EntityUser user){ return this.usersFavers.contains(user); }

    public boolean favSong(EntityUser user){ return this.usersFavers.add(user); }

    public boolean unfavSong(EntityUser user){ return this.usersFavers.remove(user); }

    @Transactional
    public Set<EntityUser> getListeners(){
        return this.usersListeners;
    }

    public boolean setAlbum(EntityAlbum album){
        if(this.album == null) {
            this.album = album;
            update();
            album.updateAlbum();
            return true;
        }

        return false;
    }

    public boolean removeFromAlbum(){
        if(this.album != null) {
            album.updateAlbum();
            this.album = null;
            update();
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void save() {
        Session s = HibernateUtils.getSessionFactory().getCurrentSession();
        s.saveOrUpdate(this.usersFavers);
        s.saveOrUpdate(this.usersLikers);
        s.saveOrUpdate(this.usersListeners);
    }


}
