require "securerandom"

timestamp = Time.now.to_i
sku = Random.rand(10000)
brands = ["nike", "adidas"]
names = ["abc", "yomommas", "yeomommas", "yourmoms"]
conditions = ["ds", "vnds"]
genders = ["male", "female"]

name = names.sample
brand = brands.sample
condition = conditions.sample
gender = genders.sample

# puts `curl -X POST 'http://localhost:8080/api/shoes' \
#   -H 'Content-Type: application/json' \
#   -d '{
#     "name": "#{name}",
#     "brand": "#{brand}",
#     "createdAt": #{timestamp},
#     "sku": #{sku},
#     "description": "description",
#     "condition": "#{condition}",
#     "gender": "#{gender}",
#     "sizes": [9, 10, 10.5, 11]
#   }'`

puts `curl -iX POST 'http://localhost:8080/api/search' \
  -H 'Content-Type: application/json' \
  -d '{
    "q": "",
    "from": 0,
    "size": 20,
    "filters": {
      "sizes": ["8","10"]
    }
  }'`
