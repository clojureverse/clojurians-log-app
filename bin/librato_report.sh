curl -u $LIBRATO_EMAIL:$LIBRATO_TOKEN \
     -d '&gauges[0][name]=CPU'\
     '&gauges[0][value]='`echo $(ps aux | awk '{s=s+$3}; END{print s}')`''\
     '&gauges[0][source]=clojurians-log'\
     '&gauges[1][name]=MEM'\
     '&gauges[1][value]='`echo $(ps aux | awk '{s=s+$4}; END{print s}')`''\
     '&gauges[1][source]=clojurians-log' \
     '&gauges[1][name]=Disk'\
     '&gauges[2][value]='`df | grep /dev/vda1 | awk '{print $5}' | sed s/%//`''\
     '&gauges[2][source]=clojurians-log' \
     -X POST https://metrics-api.librato.com/v1/metrics;
