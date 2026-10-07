package net.pokereport.luna.voice;

import net.pokereport.luna.db.Database;
import java.util.*;

/** Uses the existing MariaDB pool. No repository call is reachable from the audio handler. */
public final class VoiceRepository implements VoiceRelations.Repository {
    private final Database database;
    public VoiceRepository(Database database){this.database=database;}
    public VoiceRelations.Saved load() throws Exception {
        var edges=new HashMap<VoiceRelations.Pair,VoiceRelations.Edge>();
        var prefs=new HashMap<UUID,VoiceRelations.Preferences>();
        try(var c=database.connection();var st=c.createStatement()) {
            try(var rs=st.executeQuery("SELECT low_uuid,high_uuid,known,block_low,block_high FROM pokevoice_relationship")) {
                while(rs.next()) {
                    var p=new VoiceRelations.Pair(UUID.fromString(rs.getString(1)),UUID.fromString(rs.getString(2)));
                    if(edges.put(p,new VoiceRelations.Edge(rs.getBoolean(3),rs.getBoolean(4),rs.getBoolean(5)))!=null)
                        throw new IllegalArgumentException("Duplicate voice record");
                }
            }
            try(var rs=st.executeQuery("SELECT uuid,receive_requests,allow_known FROM pokevoice_preferences")) {
                while(rs.next())prefs.put(UUID.fromString(rs.getString(1)),new VoiceRelations.Preferences(rs.getBoolean(2),rs.getBoolean(3)));
            }
        }
        return new VoiceRelations.Saved(edges,prefs);
    }
    public void edge(VoiceRelations.Pair p,VoiceRelations.Edge e) throws Exception {
        try(var c=database.connection();var st=c.prepareStatement("INSERT INTO pokevoice_relationship(low_uuid,high_uuid,known,block_low,block_high) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE known=VALUES(known),block_low=VALUES(block_low),block_high=VALUES(block_high)")) {
            st.setString(1,p.low().toString());st.setString(2,p.high().toString());st.setBoolean(3,e.known());st.setBoolean(4,e.blockLow());st.setBoolean(5,e.blockHigh());st.executeUpdate();
        }
    }
    public void preferences(UUID u,VoiceRelations.Preferences p) throws Exception {
        try(var c=database.connection();var st=c.prepareStatement("INSERT INTO pokevoice_preferences(uuid,receive_requests,allow_known) VALUES(?,?,?) ON DUPLICATE KEY UPDATE receive_requests=VALUES(receive_requests),allow_known=VALUES(allow_known)")) {
            st.setString(1,u.toString());st.setBoolean(2,p.receiveRequests());st.setBoolean(3,p.allowKnown());st.executeUpdate();
        }
    }
}
