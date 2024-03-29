#!/bin/sh

JAVA_CMD="java -server -Xss512M -Xmx16G -cp lib/graph.jar:lib/spinja.jar:lib/ndfs.jar driver.Main $*"

if  hostname -f | grep -q -e 'fs.\.cm\.cluster' ; then
  module load prun
  echo "Running on DAS using prun."
  exec prun -np 1 $JAVA_CMD
else
  echo "Running locally. Do not use this output for evaluation benchmarks!"
  exec $JAVA_CMD
fi
