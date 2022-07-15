package com.susu.dfs.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * <p>Netty 工具类</p>
 *
 * @author sujay
 * @version 20:45 2022/2/14
 * @since JDK1.8
 */
public class NetUtils {

    private static String HOSTNAME;

    public static final String WINDOWS = "1";
    public static final String LINUX = "2";

    /**
     * <p>Description: Get local address</p>
     *
     * @author sujay
     * @param macType 机器类型
     */
    public static String getHostName(String macType) {
        if (WINDOWS.equals(macType)) {
            try {
                HOSTNAME = (InetAddress.getLocalHost()).getHostAddress();
            } catch (UnknownHostException uhe) {
                String host = uhe.getMessage();
                if (host != null) {
                    int colon = host.indexOf(':');
                    if (colon > 0) {
                        return host.substring(0, colon);
                    }
                }
                HOSTNAME = "UnknownHost";
            }
        } else if (LINUX.equals(macType)){
            try
            {
                out:for(Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();)
                {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if(networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp())
                    {
                        continue;
                    }
                    String name=networkInterface.getName();
                    if(!name.contains("docker")&&!name.contains("lo"))
                    {
                        Enumeration<InetAddress> inetAddresses=networkInterface.getInetAddresses();
                        while(inetAddresses.hasMoreElements())
                        {
                            InetAddress address=inetAddresses.nextElement();
                            HOSTNAME=address.getHostAddress();
                            if(!HOSTNAME.contains("::")&&!HOSTNAME.contains("0:0:")&&!HOSTNAME.contains("fe80"))
                            {
                                break out;
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                HOSTNAME = "UnknownHost";
            }
        } else {
            HOSTNAME = "UnknownHost";
        }
        return HOSTNAME;
    }
}
