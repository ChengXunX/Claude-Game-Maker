#!/bin/bash
cd /home/chengxun/chengxun_game_maker
set -a
source .env
set +a
exec java -jar -Xms256m -Xmx512m -XX:MaxMetaspaceSize=512m target/game-maker-1.0-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --server.port=19922 \
    --server.address=127.0.0.1
