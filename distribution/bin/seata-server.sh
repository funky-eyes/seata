#!/bin/bash
# Copyright 1999-2019 Seata.io Group.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
BASEDIR=${BASEDIR//"//"/"/"}

. ${BASEDIR}/bin/seata-setup.sh
JAVA_OPT="${JAVA_OPT} -Dspring.config.additional-location=${BASEDIR}/conf/ -Dspring.config.location=${BASEDIR}/conf/application.yml -Dlogging.config=${BASEDIR}/conf/logback-spring.xml"
JAVA_OPT="${JAVA_OPT} -jar ${BASEDIR}/target/seata-server.jar"

CMD_LINE_ARGS=$@

# start
echo "$JAVACMD ${JAVA_OPT} ${CMD_LINE_ARGS} >> /dev/null 2>&1 &"
nohup $JAVACMD ${JAVA_OPT} ${CMD_LINE_ARGS} >> /dev/null 2>&1 &
echo "seata-server is starting, you can check the ${LOG_HOME}/ *.log"
