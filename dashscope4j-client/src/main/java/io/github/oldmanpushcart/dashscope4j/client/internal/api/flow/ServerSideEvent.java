package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.Scanner;

record ServerSideEvent(String id, String event, String data) {

    public static ServerSideEvent valueOf(String body) {

        if (CommonUtils.isBlankString(body)) {
            return null;
        }

        try (final var scanner = new Scanner(body)) {
            String id = null;
            String event = null;
            String data = null;
            while (scanner.hasNextLine()) {
                final var line = scanner.nextLine();

                // 过滤注释行
                if (line.startsWith(":")) {
                    continue;
                }

                // 提取ID
                else if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                }

                // 提取event
                else if (line.startsWith("event:")) {
                    event = line.substring(6).trim();
                }

                // 提取data
                else if (line.startsWith("data:")) {
                    data = line.substring(5).trim();
                }

            }

            return new ServerSideEvent(id, event, data);
        }

    }

}
