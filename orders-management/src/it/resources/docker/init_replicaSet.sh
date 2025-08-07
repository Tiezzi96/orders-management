#mongo --eval "rs.initiate();"
mongo --quiet --eval "rs.initiate({
  _id: 'rs0',
  members: [
    { _id: 0, host: 'localhost:27017' }
  ]
})"
until mongo --eval "rs.isMaster()" | grep ismaster | grep true > /dev/null 2>&1;do sleep 1;done