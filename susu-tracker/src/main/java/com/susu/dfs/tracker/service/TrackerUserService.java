package com.susu.dfs.tracker.service;

import com.alibaba.fastjson.JSONObject;
import com.susu.dfs.common.Constants;
import com.susu.dfs.common.User;
import com.susu.dfs.common.eum.ActionEnum;
import com.susu.dfs.common.task.TaskScheduler;
import com.susu.dfs.common.utils.FileUtils;
import com.susu.dfs.common.utils.NetUtils;
import com.susu.dfs.common.utils.StringUtils;
import io.netty.channel.Channel;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>Description: Tracker 的用户管理</p>
 *
 * @author sujay
 * @version 10:14 2022/9/19
 */
public class TrackerUserService {

    /**
     *  持久化文件路径
     */
    private final String USERS_FILE_BASE_DIR;;

    private TaskScheduler taskScheduler;

    /**
     * 用户集
     */
    private Map<String, User> users;

    /**
     * 用户连接信息
     */
    private Map<String, String> channelUsers = new ConcurrentHashMap<>();

    /**
     * 用户登录token
     */
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    public TrackerUserService(String baseDir, TaskScheduler taskScheduler) {
        this.USERS_FILE_BASE_DIR = baseDir + File.separator + Constants.USER_FILE_NAME;
        this.taskScheduler = taskScheduler;
        loadReadyUsers();
        this.taskScheduler.schedule("Save users information", this::loadWriteUsers,
                Constants.WRITE_USER_FILE_INTERVAL,
                Constants.WRITE_USER_FILE_INTERVAL,
                TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::loadWriteUsers));
    }

    /**
     * 加载磁盘中的用户新
     */
    public void loadReadyUsers() {
        File file = new File(USERS_FILE_BASE_DIR);

        if (!file.exists()) {
            users = new ConcurrentHashMap<>();
            User user = new User();
            user.setSecret("susu");
            user.setUsername("susu");
            users.put("susu",user);
            return;
        }

        try {
            String usersInfo = FileUtils.readString(USERS_FILE_BASE_DIR);
            List<User> usersJson = JSONObject.parseArray(usersInfo, User.class);

            users = usersJson
                    .stream()
                    .collect(Collectors.toMap(User::getUsername, Function.identity()));

        } catch (Exception e) {
            throw new RuntimeException("Error loading authentication information：", e);
        }
    }

    /**
     * 将用户信息写入磁盘
     */
    public void loadWriteUsers() {
        String data = JSONObject.toJSONString(users.values());
        try {
            FileUtils.writeFile(USERS_FILE_BASE_DIR,true, ByteBuffer.wrap(data.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Error saving user information",e);
        }
    }

    /**
     * 添加一个用户，如果已存在用户则为修改
     */
    public void addUser(User user) {
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getSecret())) {
            return;
        }
        User containsUser = users.get(user.getUsername());

        if (containsUser == null) {
            users.put(user.getUsername(),user);
            return;
        }
        containsUser.setSecret(user.getSecret());
    }

    /**
     * <p>Description: Delete user, delete user online token </p>
     * <p>Description: 删除用户，删除用户在线Token </p>
     *
     * @param username 用户名
     */
    public User deleteUser(String username) {
        synchronized (this) {
            User user = users.remove(username);
            userTokens.remove(username);
            return user;
        }
    }

    /**
     * <p>Description: Login </p>
     * <p>Description: 用户登录 </p>
     *
     * @param channel           客户端连接
     * @param username          用户名
     * @param secret            用户秘钥
     */
    public boolean login(Channel channel, String username, String secret) {
        synchronized (this) {
            if (!users.containsKey(username)) {
                return false;
            }

            User user = users.get(username);
            if (!user.getSecret().equals(secret)) {
                return false;
            }

            String channelId = NetUtils.getChannelId(channel);
            Set<String> existsTokens = userTokens.computeIfAbsent(username, k -> new HashSet<>());
            existsTokens.add(channelId + "-" + StringUtils.getRandomString(10));
            channelUsers.put(channelId,user.getUsername());

            return true;
        }
    }

    /**
     * <p>Description: Logout </p>
     * <p>Description: 用户退出 </p>
     *
     * @param channel           客户端连接
     */
    public boolean logout(Channel channel) {
        synchronized (this) {
            String channelId = NetUtils.getChannelId(channel);
            String username = channelUsers.get(channelId);

            if (StringUtils.isEmpty(username)) {
                return false;
            }

            Set<String> existsTokens = userTokens.get(username);
            Iterator<String> iterator = existsTokens.iterator();

            while (iterator.hasNext()) {
                String token = iterator.next();
                String existsClientId = token.split("-")[0];

                if (existsClientId.equals(channelId)) {
                    iterator.remove();
                }
            }

            return true;
        }
    }

    /**
     * <p>Description: 获取用户Token </p>
     */
    public String getToken(Channel channel) {
        synchronized (this) {
            String clientId = NetUtils.getChannelId(channel);
            String username = channelUsers.get(clientId);
            if (StringUtils.isEmpty(username)) {
                return null;
            }

            Set<String> tokens = userTokens.get(username);
            for (String token : tokens) {
                String existsClientId = token.split("-")[0];
                if (existsClientId.equals(clientId)) {
                    return token;
                }
            }
            return null;
        }
    }

    /**
     * <p>Description: Receive broadcast authentication request to save token </p>
     * <p>Description: 收到广播的认证请求保存token </p>
     *
     * @param username 用户名
     * @param userToken 用户认证Token
     */
    public void setToken(String username, String userToken) {
        Set<String> existsTokens = userTokens.computeIfAbsent(username, k -> new HashSet<>());
        existsTokens.add(userToken);
    }

    /**
     * <p>Description: 该用户是否登录 </p>
     *
     * @param channel  客户端
     * @param token    认证信息
     */
    public boolean isLogin(Channel channel, String token) {
        String clientId = NetUtils.getChannelId(channel);
        String username = channelUsers.get(clientId);
        if (StringUtils.isBlank(username)) {
            return false;
        }
        User user = users.get(username);
        if (user == null) {
            return false;
        }
        Set<String> tokens = userTokens.getOrDefault(user.getUsername(), new HashSet<>());
        return tokens.contains(token);
    }

    /**
     * <p>Description: Synchronize user information </p>
     *
     * @param action Operation Type
     * @param user Users to be updated
     */
    public void syncUserEvent(User user, ActionEnum action) {
        switch (action) {
            case ADD:
            case UPDATE:
                addUser(user);
                break;
            case DELETE:
                deleteUser(user.getUsername());
                break;
            default:
                break;
        }
    }

    /**
     * <p>Description: Synchronize user information and refresh locally stored user information </p>
     *
     * @param users Users needing synchronization
     */
    public void syncUsersEvent(List<User> users) {
        this.users = users
                .stream()
                .collect(Collectors.toMap(User::getUsername, Function.identity()));
        loadWriteUsers();
    }


    public User getUser(String username) {
        return User.copy(users.get(username));
    }

    public List<User> getUsers() {
        return users.values()
                .stream()
                .map(User::copy)
                .collect(Collectors.toList());
    }

}
