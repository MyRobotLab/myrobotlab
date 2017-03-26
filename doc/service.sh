#!/bin/sh

# READ ME
# sudo su -s /bin/bash jenkins
# for service controller from a yum installed jenkins
# this file is installed in init.d which permissions chmod +x and ownership of jenkins
# [root@devnode01 init.d]# ls -al sprinkler
# -rwxr-xr-x 1 jenkins jenkins 1602 Jun  5 18:29 sprinkler
# http://www.jcgonzalez.com/linux-java-service-wrapper-example

SERVICE_NAME=sprinkler
CP=/opt/mrl/myrobotlab.jar:/opt/mrl/libraries/jar/*
MAIN_CLASS=com.daimler.rest.EntityBroker
PID_PATH_NAME=/tmp/sprinkler-pid
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -server -cp $CP $MAIN_CLASS /tmp 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup java -server -cp $CP $MAIN_CLASS /tmp 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac

exit $RETVAL
