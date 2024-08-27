package com.thanos.chain.consensus.hotstuffbft.store;

import com.thanos.chain.config.ConfigResourceUtil;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.consensus.hotstuffbft.safety.Waypoint;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;


/**
 * PersistentSafetyStorage.java description：
 *
 * @Author laiyiyu create on 2020-03-05 10:46:40
 */
public class PersistentSafetyStorage {

    public static final String EPOCH = "epoch";

    public static final String LAST_VOTED_ROUND = "last_voted_round";

    public static final String PREFERRED_ROUND = "preferred_round";

    public static final String WAYPOINT = "waypoint";

    public static abstract class Storage {

        //if key create a exist key return true at once, or else do put and return false;
        public boolean create_if_not_exists(String key, String value) {
            return !create(key, value);
        }

        //if key exist and return false, or else do put and return true;
        abstract boolean create(String key, String value);

        abstract String get(String key);

        //if key not exist and return false, or else update key and return true;
        abstract boolean set(String key, String value);
    }

    public static class InMemoryStorage extends Storage {

        HashMap<String, String> date;

        public InMemoryStorage() {
            this.date = new HashMap<>();
        }

        @Override
        public boolean create(String key, String value) {
            if (date.containsKey(key)) return false;

            date.put(key, value);
            return true;
        }

        @Override
        public String get(String key) {
            return date.get(key);
        }

        @Override
        public boolean set(String key, String value) {
            if (!date.containsKey(key)) return false;
            date.put(key, value);
            return true;
        }
    }

    public static class OnDiskStorage extends Storage {

        static SystemConfig config = ConfigResourceUtil.loadSystemConfig();

        String pathSeparator = File.separator;
        String filePath;
        String tempFilePath;
        public OnDiskStorage() {
            this.filePath = config.databaseDir() + pathSeparator + "consensus" + pathSeparator + "other" + pathSeparator + "safety_rules.txt";
            this.tempFilePath =config.databaseDir() + pathSeparator + "consensus" + pathSeparator + "other" + pathSeparator + "safety_rules_temp.txt";
            System.out.println(filePath);
            System.out.println(tempFilePath);
        }

        @Override
        public boolean create(String key, String value) {
            Map<String, String> data = read();
            if (data.containsKey(key)) return false;
            data.put(key, value);
            try {
                write(data);
            } catch (IOException e) {

            }
            return true;
        }

        @Override
        public String get(String key) {
            Map<String, String> data = read();
            return data.remove(key);
        }

        @Override
        public boolean set(String key, String value) {
            Map<String, String> data = read();
            if (!data.containsKey(key)) return false;

            data.put(key, value);
            try {
                write(data);
            } catch (IOException e) {
            }
            return true;
        }

        public Map<String, String> read() {
            createIfNotExist(this.filePath);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(this.filePath))))) {
                Map<String, String> data = new HashMap<>();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        data.put(parts[0], String.valueOf(parts[1]));
                    } else {
                        throw new RuntimeException("malformed safety file");
                    }
                }
                return data;
            } catch (IOException e) {
                throw new RuntimeException("failed to read the safety file at " + filePath, e);
            }
        }

        public void write(Map<String, String> date) throws IOException {
            createIfNotExist(tempFilePath);
            Files.write(
                    Paths.get(tempFilePath),
                    () -> date.entrySet()
                            .stream()
                            .<CharSequence>map(entry -> entry.getKey() + "=" + entry.getValue())
                            .iterator()
            );
            Files.move(Paths.get(tempFilePath), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        }

        public File createIfNotExist(String filePath)  {
            File file = new File(filePath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return file;
        }
    }


    private Storage storage;

    public PersistentSafetyStorage(boolean inMemory) {
        if (inMemory) {
            this.storage = new InMemoryStorage();
        } else {
            this.storage = new OnDiskStorage();
        }
        initStore();
    }

    private void initStore() {
        storage.create_if_not_exists(EPOCH, String.valueOf(1));
        storage.create_if_not_exists(LAST_VOTED_ROUND, String.valueOf(0));
        storage.create_if_not_exists(PREFERRED_ROUND, String.valueOf(0));
        storage.create_if_not_exists(WAYPOINT, new Waypoint().toJSONString());
    }

    public void setEpoch(long epoch) {
        storage.set(EPOCH, Long.toString(epoch));
    }

    public long getEpoch() {
        String result = this.storage.get(EPOCH);
        return result != null? Long.parseLong(result) : 1;
    }

    public void setLastVotedRound(long round) {
        storage.set(LAST_VOTED_ROUND, Long.toString(round));
    }

    public long getLastVotedRound() {
        String result = this.storage.get(LAST_VOTED_ROUND);
        return result != null? Long.parseLong(result) : 0;
    }

    public void setPreferredRound(long round) {
        storage.set(PREFERRED_ROUND, Long.toString(round));
    }

    public long getPreferredRound() {
        String result = this.storage.get(PREFERRED_ROUND);
        return result != null? Long.parseLong(result) : 0;
    }

    public Waypoint getWaypoint() {
        return Waypoint.build(storage.get(WAYPOINT));
    }

    public void setWaypoint(Waypoint waypoint) {
        storage.set(WAYPOINT, waypoint.toJSONString());
    }
}
