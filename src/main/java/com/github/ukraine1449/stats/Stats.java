package com.github.ukraine1449.stats;

import com.github.ukraine1449.stats.Blocks.BlockBreak;
import com.github.ukraine1449.stats.Blocks.BlockPlace;
import com.github.ukraine1449.stats.Death.Kills;
import com.github.ukraine1449.stats.Menu.MenuHandler;
import com.github.ukraine1449.stats.Menu.StatsCommand;
import com.github.ukraine1449.stats.Player.CachedPlayer;
import com.github.ukraine1449.stats.Player.Exp;
import com.github.ukraine1449.stats.Player.PlayerWalk;
import com.github.ukraine1449.stats.PlayerEvents.JoinLeave;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public final class Stats extends JavaPlugin {
    //Creates instance to reference in other classes like CachedPlayer, then creates online player list and list of all played UUIDs
    public static Stats instance;
    public ArrayList<Player> onlinePlayers =  new ArrayList<>();
    public ArrayList<String> UUIDs = new ArrayList<>();
    @Override
    public void onEnable() {//Sets instance of class as this, then creates db table and loads players that have played before(if any)
        instance =this;
        try {
            createTableUserList();
            loadAllPlayers();
        } catch (Exception e) {
            e.printStackTrace();
        }//Registers all commands and events.
        getCommand("stats").setExecutor(new StatsCommand(this));
        getServer().getPluginManager().registerEvents(new BlockBreak(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlace(this), this);
        getServer().getPluginManager().registerEvents(new Kills(), this);
        getServer().getPluginManager().registerEvents(new MenuHandler(), this);
        getServer().getPluginManager().registerEvents(new Exp(), this);
        getServer().getPluginManager().registerEvents(new PlayerWalk(), this);
        getServer().getPluginManager().registerEvents(new JoinLeave(this), this);
    }

    @Override
    public void onDisable() {
        for(Player p : onlinePlayers){//loops through all online players, sends data to DB.
            CachedPlayer cp = CachedPlayer.get(p);
            cp.loadToDB();
            onlinePlayers.remove(p);
            cp.remove();
        }
    }
    public Connection getConnection() throws Exception{//Base con string for SQL
        String ip = getConfig().getString("MySQL.ip");
        String password = getConfig().getString("MySQL.password");
        String username = getConfig().getString("MySQL.username");
        String dbn = getConfig().getString("MySQL.database name");//these 4 strings get the login info from config.yml file, and use that for DB connections
        try{
            String driver = "com.mysql.jdbc.Driver";
            String url = "jdbc:mysql://"+ ip + ":3306/" + dbn;
            System.out.println(url);
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("Connected");
            return conn;
        }catch(Exception e){
            System.out.println("Unable to connect to SQL server.");
        }
        return null;
    }
    public void loadAllPlayers(){//Loads all UUIDs to list, to know the allready pre-played ones.
        Bukkit.getScheduler().runTaskAsynchronously(getServer().getPluginManager().getPlugin("Stats"), new Runnable() {
            @Override
            public void run () {
                try {
                    Connection con = getConnection();
                    PreparedStatement statement = con.prepareStatement("SELECT * FROM userList");
                    ResultSet result = statement.executeQuery();
                    while(result.next()){
                        UUIDs.add(result.getString("UUID"));
                    }
                    con.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }//Creates the table
    public void createTableUserList()throws Exception{
        try{
            Connection con = getConnection();
            PreparedStatement create = con.prepareStatement("CREATE TABLE IF NOT EXISTS userList(UUID varchar(255),kills int,deaths int,exp int,mined int,placed int,walked int, PRIMARY KEY (UUID))");
            create.executeUpdate();
            con.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void playerJoinQuery(String UUID){
        try{//executed when a player is joining in playerJoinEvent, basically if the players UUID isnt already in the database it adds it with all stats of 0
            Connection con = getConnection();
            PreparedStatement posted = con.prepareStatement("INSERT INTO userList(UUID, kills, deaths, exp, mined, placed, walked) VALUES ('"+UUID+"', 0, 0, 0, 0, 0, 0)ON DUPLICATE KEY UPDATE UUID='"+UUID+"'");
            posted.executeUpdate();
            con.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
//TODO Stats: Kills, Deaths, Expirience gained total, Blocks mined, Blocks placed, Walked
//TODO add command to edit stats, stat rewards