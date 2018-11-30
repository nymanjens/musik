#!/bin/sh
# To install musik as a service:
# - move this file to /etc/init.d/musik
# - update SERVICE_ROOT
# - run:
#     sudo chmod +x /etc/init.d/musik
#     sudo update-rc.d musik defaults # start at startup

### BEGIN INIT INFO
# Provides:          musik
# Required-Start:    $local_fs $remote_fs $network $syslog $named
# Required-Stop:     $local_fs $remote_fs $network $syslog $named
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Starts musik
# Description:       Starts musik using start-stop-daemon
### END INIT INFO

USER=root
HOME=/root
export USER HOME

### settings ##
SERVICE_ROOT=/path/to/musik
RUNNING_PID=$SERVICE_ROOT/RUNNING_PID

### helper functions ###
kill_running() {
  if [ -f "$RUNNING_PID" ]; then
    pid=`cat "$RUNNING_PID"`
    echo "Killing process $pid found in $RUNNING_PID"
    kill -15 "$pid"

    echo "Waiting for $RUNNING_PID to disappear..."
    for i in `seq 1 40`; do
      if [ -f "$RUNNING_PID" ]; then
        if ps -p $pid > /dev/null ; then
          sleep 0.5
        else
          echo "Process stopped without removing $RUNNING_PID" && echo
          echo "Removing $RUNNING_PID..."
          rm -f "$RUNNING_PID"
          echo "Done" && echo
          break
        fi
      else
        echo "Done" && echo
        break
      fi
    done

    if [ -f "$RUNNING_PID" ]; then
      echo "Timed out" && echo
    fi
  fi
}

### service implementation ###
case "$1" in
  start|restart)
    kill_running

    echo "Starting musik..." && echo

    su pi -c "cd $SERVICE_ROOT && bin/server -Dhttp.port=8782" > /tmp/musik-logs 2>&1 &

    echo "Waiting for $RUNNING_PID to appear"
    for i in `seq 1 20`; do
      if [ ! -f "$RUNNING_PID" ]; then
        sleep 0.5
      else
        break
      fi
    done

    if [ -f "$RUNNING_PID" ]; then
      pid=`cat "$RUNNING_PID"`
      echo "Done. Process $pid was started." && echo
    else
      echo "Timed out" && echo
    fi
    ;;

  stop)
    echo "Stopping musik..." && echo
    kill_running
    ;;

  *)
    echo "Usage: service musik {start|stop|restart}"
    exit 1
    ;;
esac

exit 0
