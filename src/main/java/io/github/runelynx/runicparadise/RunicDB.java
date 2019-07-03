package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;

public class RunicDB {

    static private Connection openConnection() {
        MySQL MySQL = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());

        try {
            final Connection dbCon = MySQL.openConnection();
            return dbCon;
        } catch (Exception z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed RunicDB.openConnection - " + z.getMessage());
            return null;
        }
    }

    static private void closeconnection(CallableStatement cs, Connection dbCon) {

        try {
            cs.close();
            dbCon.close();
        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed RunicDB.closeconnection - " + z.getMessage());
        }

    }

    static public int callSP_Count_McmmoUsers(String puuid) {
        final Connection dbCon = openConnection();

        try {
            String simpleProc = "{ call Count_McmmoUsers(?) }";
            CallableStatement cs = dbCon.prepareCall(simpleProc);
            cs.setString("userid", puuid);
            cs.registerOutParameter(2, Types.SMALLINT);
            cs.execute();

            int result = cs.getInt(2);

            closeconnection(cs, dbCon);

            return result;
        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed RunicDB.callSP_Count_McmmoUsers - " + z.getMessage());
            return 2;
        }
    }

    static public int callSP_Add_McmmoUser(String userName, String puuid) {
        final Connection dbCon = openConnection();

        try {
            String simpleProc = "{ call Add_McmmoUser(?, ?) }";
            CallableStatement cs = dbCon.prepareCall(simpleProc);
            cs.setString("userName", userName);
            cs.setString("uuid", puuid);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();

            int result = cs.getInt(3);

            closeconnection(cs, dbCon);

            return result;
        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed RunicDB.callSP_Add_McmmoUser - " + z.getMessage());
            return 0;
        }
    }

    static public boolean callSP_Add_McmmoUserRecords(int mcmmoUserID) {
        final Connection dbCon = openConnection();

        try {
            String simpleProc = "{ call Add_McmmoUserRecords(?) }";
            CallableStatement cs = dbCon.prepareCall(simpleProc);
            cs.setInt("userid", mcmmoUserID);
            cs.execute();

            closeconnection(cs, dbCon);

            return true;
        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed RunicDB.callSP_Add_McmmoUserRecords - " + z.getMessage());
            return false;
        }
    }

}
