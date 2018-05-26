package es.eina.sql.entities;

import es.eina.RestApp;
import es.eina.crypt.Crypter;
import es.eina.sql.utils.HibernateUtils;
import es.eina.utils.StringUtils;
import es.eina.utils.UserUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.NaturalId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.transaction.Transactional;
import java.sql.Date;
import java.util.*;

@Entity(name="user")
@Table(name="users")
public class EntityUser extends EntityBase {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @NaturalId
    @Column(name = "nick", nullable = false, unique = true)
    private String nick;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name="mail", nullable = false)
    private String mail;

    @Column(name="pass", nullable = false)
    private String pass;

    @Column(name="birth_date", nullable = false)
    private Date birthDate;

    @Column(name="bio", nullable = false)
    private String bio;

    @Column(name="country", nullable = false)
    private String country;

    @Column(name="register_date", nullable = false)
    private long register_date;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private EntityToken token;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private EntityUserValues userValues;


    @OneToMany(mappedBy = "user", cascade=CascadeType.ALL)
    private Set<EntityAlbum> albums = new HashSet<>();

    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(
        name = "user_liked_songs",
        joinColumns = { @JoinColumn(name = "user_id")},
        inverseJoinColumns = {@JoinColumn(name = "song_id")}
    )
    Set<EntitySong> songsLiked = new HashSet<>();

    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(
            name = "user_faved_songs",
            joinColumns = { @JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "song_id")}
    )
    Set<EntitySong> songsFaved = new HashSet<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    Set<EntityUserSongData> songsListened = new HashSet<>();

    /**
     * DO NOT use this method as it can only be used by Hibernate
     */
    public EntityUser(){

    }

    public EntityUser(String nick, String username, String mail, String pass, Date birthDate, String bio, String country) {
        this.nick = nick;
        this.username = username;
        this.mail = mail;
        this.pass = pass;
        this.birthDate = birthDate;
        this.bio = bio;
        this.country = country;
        this.register_date = System.currentTimeMillis();
    }

    public void updateToken(){
        if(this.token != null){
            this.token.updateToken();
        }else{
            this.token = new EntityToken(this);
        }
    }

    public void verifyAccount(){
        if(userValues == null){
            userValues = new EntityUserValues(this);
        }

        userValues.setVerified(true);
    }

    public int unverifyAccount(){
        if(userValues == null) return -2;

        userValues.setVerified(false);
        if(userValues.cleanUp()){
            int code = userValues.deleteEntity();
            if(code == 0) {
                userValues = null;
            }
            return code;
        }
        return 0;
    }

    public void makeAdmin(){
        if(userValues == null){
            userValues = new EntityUserValues(this);
        }

        userValues.setAdmin(true);
    }

    public int demoteAdmin(){
        if(userValues == null) return -2;

        userValues.setAdmin(false);
        if(userValues.cleanUp()){
            int code = userValues.deleteEntity();
            if(code == 0) {
                userValues = null;
            }
            return code;
        }
        return 0;
    }

    public int updateUser(String key, Object value){
        int code = -1;
        if("username".equals(key)){
            if(value instanceof String){
                username = (String) value;
                code = 0;
            }
        }else if("mail".equals(key)){
            if(value instanceof String){
                mail = (String) value;
                code = 0;
            }
        }else if("bio".equals(key)){
            if(value instanceof String){
                bio = (String) value;
                code = 0;
            }
        }else if("birth_date".equals(key)){
            if(value instanceof Number){
                this.birthDate = new Date((Long) value);
                code = 0;
            }
        }else if("pass".equals(key)){
            if(value instanceof JSONObject){
                JSONObject obj = (JSONObject) value;
                if(obj.has("pass0") && obj.has("pass1") && obj.has("old_pass")) {
                    try {
                        String pass = obj.getString("old_pass");
                        String pass0 = obj.getString("pass0");
                        String pass1 = obj.getString("pass1");
                        if(StringUtils.isValid(pass0) && pass0.equals(pass1) &&
                                UserUtils.checkPassword(this, pass)){
                            this.pass = Crypter.hashPassword(pass0, false);
                            code = 0;
                        }else{
                            code = -2;
                        }
                    }catch (JSONException ignored){}
                }
            }
        }

        return code;
    }

    public int deleteToken(){
        if(this.token != null) {
            token.removeUser();
            EntityToken token = this.token;
            this.token = null;
            Session s = HibernateUtils.getSession();
            Transaction t = s.beginTransaction();
            try {
                s.delete(token);
                t.commit();
            }catch(Exception e){
                t.rollback();
                e.printStackTrace();
                return -1;
            }

            return 0;
        }
        return -2;
    }

    public Long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getUsername() {
        return username;
    }

    public String getMail() {
        return mail;
    }

    public String getPass() {
        return pass;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getBio() {
        return bio;
    }

    public String getCountry() {
        return country;
    }

    public long getRegisterDate() {
        return register_date;
    }

    public EntityToken getToken() {
        return token;
    }

    public EntityUserValues getUserValues() {
        return userValues;
    }

    public boolean isAdmin() {
        return userValues != null && userValues.isAdmin();
    }

    public boolean isVerified() {
        return userValues != null && userValues.isVerified();
    }

    /*public Set<EntitySong> getSongs() {
        return songs;
    }*/

    @Transactional
    public JSONArray getUserSongs() {
        JSONArray songs = new JSONArray();
        RestApp.getInstance().getLogger().severe("Length: " + this.albums.size());
        for (EntityAlbum album : this.albums) {
            JSONArray albumSongs = album.getSongsAsArray();
            for(int i = 0; i < albumSongs.length(); i++) {
                songs.put(albumSongs.get(i));
            }
        }
        return songs;
    }

    @Transactional
    public boolean isSongLiked(EntitySong song){
        Session s = HibernateUtils.getSession();
        Transaction t = s.beginTransaction();
        boolean b = this.songsLiked.contains(song);
        t.commit();
        return b;
    }

    public boolean likeSong(EntitySong song){ return this.songsLiked.add(song); }

    public boolean unlikeSong(EntitySong song){ return this.songsLiked.remove(song); }

    @Transactional
    public boolean isSongFaved(EntitySong song){
        Session s = HibernateUtils.getSession();
        Transaction t = s.beginTransaction();
        boolean b = this.songsFaved.contains(song);
        t.commit();
        return b;
    }

    public boolean favSong(EntitySong song){ return this.songsFaved.add(song); }

    public boolean unfavSong(EntitySong song){ return this.songsFaved.remove(song);}

    @Transactional
    public boolean listenSong(EntitySong song){
        Session s = HibernateUtils.getSession();
        //song.getListeners().add(this);
        Transaction t = s.beginTransaction();
        boolean b = this.songsListened.add(new EntityUserSongData(this, song));
        t.commit();
        return b;
    }
}
