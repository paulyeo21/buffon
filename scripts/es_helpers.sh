#!/bin/bash

# curl -X DELETE localhost:9200/shoe
# curl -X PUT "localhost:9200/shoe" -d \
# '{
#   "settings": {
#     "number_of_shards": 1
#   },
#   "mappings": {
#     "doc": {
#       "properties": {
#         "name": {
#           "type": "text"
#         },
#         "brand": {
#           "type": "text"
#         },
#         "createdAt": {
#           "type": "date",
#           "format": "epoch_millis"
#         },
#         "sku": {
#           "type": "long"
#         }
#       }
#     }
#   }
# }'

# curl -X PUT 'localhost:9200/shoe' \
# -H 'Content-Type: application/json' \
# -d \
# '{
#   "name": "air force 1",
#   "brand": "nike",
#   "createdAt": 100120120,
#   "sku": 1111111
# }'

# curl "localhost:9200/_search?pretty" -d \
# '{
#   "query": {
#     "term": {
#       "brand": "nike"
#     }
#   }
# }'

# curl "localhost:9200/_search?pretty" -d \
# '{
#   "query": {
#     "match": {
#       "name": {
#         "query": "air",
#         "fuzziness": "AUTO"
#       }
#     }
#   }
# }'

# curl "localhost:9200/_search?pretty" -d \
# '{
#   "query": {
#     "multi_match": {
#       "query": "nike",
#       "fields": ["name", "brand"],
#       "fuzziness": "AUTO"
#     }
#   }
# }'

# curl "localhost:9200/_search?pretty" -d \
# '{
#   "query": {
#     "bool": {
#       "must": [
#         { "match_all": { }}
#       ],
#       "filter": [
#         { "range": { "timestamp": { "gte": "1525714263000" }}}
#       ]
#     }
#   }
# }'


