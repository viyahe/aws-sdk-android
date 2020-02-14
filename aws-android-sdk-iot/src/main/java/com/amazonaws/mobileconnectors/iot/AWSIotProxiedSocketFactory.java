/**
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.mobileconnectors.iot;

import com.amazonaws.AmazonClientException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.Proxy.Type;
import javax.net.ssl.SSLSocketFactory;

public class AWSIotProxiedSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory socketFactory;
    private final Proxy proxy;

    public AWSIotProxiedSocketFactory(SSLSocketFactory delegate, String proxyHost, int proxyPort){
        this.socketFactory = delegate;
        this.proxy = new Proxy(Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
    }

    public AWSIotProxiedSocketFactory(String proxyHost, int proxyPort) {
        this.socketFactory = null;
        this.proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return socketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socketFactory.getSupportedCipherSuites();
    }

    public Socket createSocket() throws IOException {
        Socket sock = new Socket(this.proxy);
        String proxyConnect = "CONNECT ";
        proxyConnect.concat("\n\n");
        sock.getOutputStream().write(proxyConnect.getBytes());
        byte[] tmpBuffer = new byte[512];
        InputStream socketInput = sock.getInputStream();
        int len = socketInput.read(tmpBuffer, 0, tmpBuffer.length);
        if (len == 0) {
            throw new SocketException("Invalid response from proxy");
        } else {
            String proxyResponse = new String(tmpBuffer, 0, len, "UTF-8");
            if (proxyResponse.indexOf("200") != -1) {
                if (socketInput.available() > 0) {
                    socketInput.skip((long)socketInput.available());
                }

                return sock;
            } else {
                throw new AmazonClientException("Fail to create Socket");
            }
        }
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return socketFactory.createSocket(socket, s, i, b);
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        return socketFactory.createSocket(s, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
        return socketFactory.createSocket(s, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return socketFactory.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return socketFactory.createSocket(inetAddress, i, inetAddress1, i1);
    }

    private static class Base64 {
        private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        private Base64() {
        }

        public static String encode(byte[] buf) {
            int size = buf.length;
            char[] ar = new char[(size + 2) / 3 * 4];
            int a = 0;

            byte b2;
            byte mask;
            for(int i = 0; i < size; ar[a++] = ALPHABET[b2 & mask]) {
                byte b0 = buf[i++];
                byte b1 = i < size ? buf[i++] : 0;
                b2 = i < size ? buf[i++] : 0;
                mask = 63;
                ar[a++] = ALPHABET[b0 >> 2 & mask];
                ar[a++] = ALPHABET[(b0 << 4 | (b1 & 255) >> 4) & mask];
                ar[a++] = ALPHABET[(b1 << 2 | (b2 & 255) >> 6) & mask];
            }

            switch(size % 3) {
                case 1:
                    --a;
                    ar[a] = '=';
                case 2:
                    --a;
                    ar[a] = '=';
                default:
                    return new String(ar);
            }
        }
    }
}
